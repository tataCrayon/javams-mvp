package com.crayon.register.service;

import com.crayon.register.model.ServerInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 注册服务
 *
 * @author crayon
 * @version 1.0
 * @date 2025/4/7
 */
@Slf4j
@Service
public class RegistryService {

    /**
     * 简单服务注册表-使用Map存储服务实例信息
     *
     * <p>
     * 使用3个Map模拟3个节点，模拟集群存储，使用ConcurrentHashMap支持并发
     * </p>
     * <p>
     * 读写分离，简单的“状态转移”操作
     * Master写入数据，从Master同步数据到Slave
     * </p>
     * <p>
     * 企业级实际使用需的同步方法常用的有Raft共识算法
     * 解决 1. Leader election 领导选举
     * 2. Log replication 日志复制
     * 3. Safety 安全性
     * 简单实现这里没有体现
     * </p>
     */
    private Map<String, List<ServerInstance>> serviceRegistryMaster = new ConcurrentHashMap<>();
    private Map<String, List<ServerInstance>> serviceRegistrySlave1 = new ConcurrentHashMap<>();
    private Map<String, List<ServerInstance>> serviceRegistrySlave2 = new ConcurrentHashMap<>();


    /**
     * 使用读写锁，简单实现强一致性
     * <p>
     * 按服务名分配锁，尽可能的小粒度以应对频繁更改的服务示例
     * </p>
     * <p>
     * 在实际生产中，企业运用的分布式架构需要使用分布式锁来实现
     * </p>
     */
    private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    /**
     * 异步同步线程池
     */
    private final ExecutorService syncExecutor = Executors.newFixedThreadPool(2);

    /**
     * 注册服务
     * <p>
     * 注册表同步可以改成异步
     *
     * @param serviceName
     * @param serverInstance
     */
    public void register(String serviceName, ServerInstance serverInstance) {
        ReentrantReadWriteLock lock = locks.computeIfAbsent(serviceName, k -> new ReentrantReadWriteLock());
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            serverInstance.setStatus(ServerInstance.Status.ONLINE); // 默认在线
            serverInstance.setLastHeartbeat(System.currentTimeMillis()); // 注册时更新心跳
            serviceRegistryMaster.computeIfAbsent(serviceName, k -> new ArrayList<>())
                    .add(serverInstance);
            log.info("Registered to Master: {} - {}", serviceName, serverInstance);
            // 异步到 Slave
            syncToSlavesAsync(serviceName, serverInstance);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 同步更新从节点
     * @param serviceName
     * @param serverInstance
     */
    private void syncToSlaves(String serviceName, ServerInstance serverInstance) {
        // 同步到 Slave1
        serviceRegistrySlave1.computeIfAbsent(serviceName, k -> new ArrayList<>())
                .add(serverInstance);
        log.info("Synced to Slave1: {} - {}", serviceName, serverInstance);

        // 同步到 Slave2
        serviceRegistrySlave2.computeIfAbsent(serviceName, k -> new ArrayList<>())
                .add(serverInstance);
        log.info("Synced to Slave2: {} - {}", serviceName, serverInstance);
    }

    /**
     * 异步同步到 Slave
     */
    private void syncToSlavesAsync(String serviceName, ServerInstance serverInstance) {
        syncExecutor.submit(() -> {
            // 同步到 Slave1
            serviceRegistrySlave1.computeIfAbsent(serviceName, k -> new ArrayList<>())
                    .add(serverInstance);
            log.info("Synced to Slave1: {} - {}", serviceName, serverInstance);

            // 同步到 Slave2
            serviceRegistrySlave2.computeIfAbsent(serviceName, k -> new ArrayList<>())
                    .add(serverInstance);
            log.info("Synced to Slave2: {} - {}", serviceName, serverInstance);
        });
    }

    /**
     * 查询服务实例
     * <p>
     * 多节点读取需要负载均衡
     *
     * @param serviceName
     * @return
     */
    public List<ServerInstance> getServiceInstancesByName(String serviceName) {
        ReentrantReadWriteLock lock = locks.computeIfAbsent(serviceName, k -> new ReentrantReadWriteLock());
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        readLock.lock();
        try {
            // 从任一个Slave中获取服务实例
            List<ServerInstance> instances = serviceRegistrySlave1.getOrDefault(serviceName, Collections.emptyList());
            log.info("Discovered from Slave1: {} - {}", serviceName, instances);
            // 返回副本，避免外部修改
            return new ArrayList<>(instances);
        } finally {
            readLock.unlock();
        }
    }


    /**
     * 建议的心跳处理方式为被动检测。
     * <p>
     * 从节点的更新使用异步效率更高
     *
     * @param instance
     */
    public void heartbeat(ServerInstance instance) {
        String serverName = instance.getServerName();
        List<ServerInstance> instances = getServiceInstancesByName(serverName);
        if (instances == null) {
            register(serverName, instance);
            return;
        }
        // 遍历获取对应实例，并更新状态
        ReentrantReadWriteLock lock = locks.computeIfAbsent(serverName, k -> new ReentrantReadWriteLock());
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            // 还是遵从 Master-Slave ，更新 Master
            List<ServerInstance> masterInstances = serviceRegistryMaster.get(serverName);
            if (masterInstances != null) {
                for (ServerInstance existing : masterInstances) {
                    if (existing.getId().equals(instance.getId())) {
                        existing.setLastHeartbeat(System.currentTimeMillis());
                        existing.setStatus(ServerInstance.Status.ONLINE);
                        log.info("Heartbeat updated: {}", existing);
                        // 异步同步到 Slave
                        syncToSlavesAsync(serverName, existing);
                        break;
                    }
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 定时检查心跳，移除超时实例
     */
    @Scheduled(fixedRate = 60000) // 每 60 秒检查
    public void checkHeartbeats() {
        serviceRegistryMaster.forEach((serviceName, instances) -> {
            ReentrantReadWriteLock lock = locks.computeIfAbsent(serviceName, k -> new ReentrantReadWriteLock());
            ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
            writeLock.lock();
            try {
                long now = System.currentTimeMillis();
                instances.removeIf(instance -> {
                    if (now - instance.getLastHeartbeat() > 60000) { // 60 秒超时
                        instance.setStatus(ServerInstance.Status.OFFLINE);
                        log.info("Service offline: {} - {}", serviceName, instance);
                        syncToSlavesAsync(serviceName, instance); // 同步下线状态
                        return true;
                    }
                    return false;
                });
            } finally {
                writeLock.unlock();
            }
        });
    }
}
