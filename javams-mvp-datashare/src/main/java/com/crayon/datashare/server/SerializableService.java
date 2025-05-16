package com.crayon.datashare.server;

import com.crayon.datashare.model.PersistenceData;
import com.crayon.datashare.model.ShareData;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 序列化服务
 *
 * <p>
 * 序列化：采用快照+日志方式
 * 按数据量策略进行快照
 * <p>
 * 文件内容:类Redis融合方案
 * 快照内容+日志内容。文件前面是RDB格式，后面是AOF格式
 * <p>
 * RDB格式：
 * AOF格式
 * <p>
 * 文件大小处理：采用保留数据最终状态的压缩方案
 *
 * <p>
 * 恢复机制：
 *
 * @author crayon
 * @version 1.0
 * @date 2025/5/15
 */
public class SerializableService {

    /**
     * 假设的日志文件名
     */
    private static final String MASTER_LOG_FILE = System.getProperty("user.dir") + "/wal/master_wal.log";

    /**
     * 日志追加
     * <p>
     * 简化的日志格式，实际应该至少有操作类型、时间戳、序列号、状态码，
     * 数据库的话会有数据库的一些信息，如数据库名字、server id等
     * </p>
     * 生产日志会有压缩、刷盘等操作，这里简化了
     */
    public boolean appendLog(String operaType, String key, ShareData value) {
        PersistenceData persistenceData = new PersistenceData.Builder()
                .operaType(operaType)
                .key(key)
                .shareData(value)
                .build();
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(MASTER_LOG_FILE, true)))) {
            // out.flush(); // 可以考虑更频繁的flush或根据策略fsync
            out.println(persistenceData.toString());
            return true;
        } catch (IOException e) {
            System.err.println("Error writing to WAL: " + e.getMessage());
            return false;
        }
    }
}