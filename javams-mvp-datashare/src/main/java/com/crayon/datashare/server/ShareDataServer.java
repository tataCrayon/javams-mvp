package com.crayon.datashare.server;

import com.crayon.datashare.model.ShareData;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 简单的共享数据存储中心
 *
 * <p>
 * 功能如下:
 * <p>
 * API-获取数据
 * API-存储数据
 * API-注册信息
 * <p>
 * 数据量到了应规模时进行序列化快照，存储数据时日志追加
 *
 * @author crayon
 * @version 1.0
 * @date 2025/5/14
 */
public class ShareDataServer {

    /**
     * 使用ConcurrentHashMap存储共享数据
     * <p>
     * keyName -> ShareData
     * </p>
     * <p>
     * 集群做读写分离设计，Leader-Follower 模型 。
     * master写同步到slave,slave节点读,负载均衡采用随机策略。
     * </p>
     * <p>
     * 数据容量暂不设置上限与对应清理机制。
     * </p>
     * <p>
     * 没有选举机制，也没有逻辑时钟
     * </p>
     */
    private static ConcurrentHashMap<String, ShareData> shareDataMaster = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, ShareData> shareDataSlave1 = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, ShareData> shareDataSlave2 = new ConcurrentHashMap<>();

    /**
     * 用于随机获取从节点
     */
    private static Random random = new Random();

    /**
     * <p>
     * keyName -> ReentrantReadWriteLock
     * </p>
     * 线程安全方案一：
     * <p>
     * 使用读写锁控制共享数据安全。
     * ConcurrentHashMap 操作数据是安全的,但是共享数据内容是可变的（Mutable）。
     * 当需要组合多个ConcurrentHashMap操作时，其是不安全的。
     * 其他线程可能在ConcurrentHashMap多个操作之间，对可变对象进行更改。
     * <p>
     * 因此需要读写锁来保证写入时候数据安全。
     * 在共享数据中，因为原子操作为：写数据+日志追加，所以更需要使用锁来控制。
     * <p>
     * 在分布式系统中，共享数据中心本身常被作为分布式锁使用。
     * <p>
     * 如果不是需要WAL，其实可以通过不可变对象（Immutable Objects）来消除数据共享来简化并发问题
     * </p>
     */
    private static ConcurrentHashMap<String, ReentrantReadWriteLock> readWriteLocks = new ConcurrentHashMap<>();


    /**
     * 订阅者集合
     * <p>
     * 采取一次性触发机制（One-time Trigger），省去心跳检测的麻烦
     * 每次通知订阅者时，会从集合中移除订阅者，订阅者每次需要重新注册
     * 比如在调用get时重新注册
     * <p>
     * 订阅者也可以封装成一个对象，这里简单一点=ip:port
     * <p>
     * keyName -> Set<ip:port>
     * 使用线程安全的Set,如 ConcurrentHashMap.newKeySet()
     * </p>
     */
    private static ConcurrentHashMap<String, Set<String>> subscribers = new ConcurrentHashMap<>();


    /**
     * Watcher
     */
    private static Notifier notifier = new Notifier();


    /**
     * 序列化服务
     * 日志操作，数据恢复（暂无）等
     */
    private static SerializableService serializableService = new SerializableService();

    /**
     * 获取共享数据
     * 采取一次性触发机制（One-time Trigger）由Server完成
     *
     * @param key
     * @param ipPort (可选) 客户端标识，用于重新注册Watcher
     * @param watch  (可选) 是否要设置Watcher
     * @return
     */
    public ShareData get(String key, String ipPort, boolean watch) {
        ReentrantReadWriteLock readWriteLock = readWriteLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
        readWriteLock.readLock().lock();
        try {
            ConcurrentHashMap<String, ShareData> readNode = getReadNode();
            ShareData shareData = readNode.get(key);
            if (watch && null != ipPort && !"".equals(ipPort) && null != shareData) {
                register(key, ipPort);
            }
            return shareData;
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * 注册订阅者
     *
     * @param key
     * @param ipPort
     */
    public void register(String key, String ipPort) {
        // 使用ConcurrentHashMap.newKeySet() 创建一个线程安全的Set
        subscribers.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(ipPort);
    }

    /**
     * 添加共享数据
     * 组合 日志追加 + 添加 + 集群同步
     * <p>
     * 原子操作设计：
     * 一般这种带集群同步的标准方案是共识算法（Consensus Algorithm）。太复杂了，搞不来。
     *
     * </p>
     *
     * @param key
     * @param value
     */
    public boolean set(String key, ShareData value) {
        ReentrantReadWriteLock readWriteLock = readWriteLocks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
        readWriteLock.writeLock().lock();
        try {
            // 1、写入日志 WAL
            boolean logSuccess = serializableService.appendLog(OperaTypeEnum.SET.getType(), key, value);
            if (!logSuccess) {
                return false;
            }
            // 2、写入内存Master
            shareDataMaster.put(key, value);
            /**
             * 3、集群同步
             * 简单模拟，没有处理网络失败、异步、其他复杂ack机制等
             */
            syncToSlave(key, value);
            // 4、通知订阅者，从注册表移除
            // 获取并移除，实现一次性触发
            Set<String> currentSubscribers = subscribers.remove(key);
            if (currentSubscribers != null && !currentSubscribers.isEmpty()) {
                for (String subscriberIpPort : currentSubscribers) {
                    // 实际应用中，这里会通过网络连接向客户端发送通知
                    notifier.notify(subscriberIpPort, key, OperaTypeEnum.CHANGE.getType());
                }
            }
        } catch (Exception e) {
            // 实际生产需要回滚等事务操作、日志记录等
            return false;
        } finally {
            readWriteLock.writeLock().unlock();
        }
        return true;
    }

    /**
     * 集群同步
     * <p>
     * 只在set操作中调用
     *
     * @param key
     * @param value
     */
    private void syncToSlave(String key, ShareData value) {
        shareDataSlave1.put(key, value); // 模拟同步到slave1
        shareDataSlave2.put(key, value); // 模拟同步到slave2
    }


    /**
     * 50%概率随机取节点
     *
     * @return
     */
    private ConcurrentHashMap<String, ShareData> getReadNode() {
        return random.nextBoolean() ? shareDataSlave1 : shareDataSlave2;
    }

}
