package com.crayon.datashare.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 内存共享数据的序列化对象
 *
 * @author crayon
 * @version 1.0
 * @date 2025/5/15
 */
@Data
public class PersistenceData implements Serializable {

    /**
     * 数据序列化时间
     */
    private Long serialDateTime;

    /**
     * 操作类型
     */
    private String operaType;

    /**
     * 数据key
     */
    private String key;

    /**
     * 共享数据
     */
    private ShareData shareData;

    public PersistenceData(Builder builder) {
        this.key = builder.key;
        this.shareData = builder.shareData;
        this.operaType = builder.operaType;
        this.serialDateTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "PersistenceData{" +
                "serialDateTime=" + serialDateTime +
                ", operaType='" + operaType + '\'' +
                ", key='" + key + '\'' +
                ", shareData=" + shareData +
                '}';
    }

    // 建造者模式
    public static class Builder {
        private String key;
        private ShareData shareData;

        private String operaType;

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder shareData(ShareData shareData) {
            this.shareData = shareData;
            return this;
        }

        public Builder operaType(String operaType) {
            this.operaType = operaType;
            return this;
        }

        public PersistenceData build() {
            return new PersistenceData(this);
        }
    }

}
