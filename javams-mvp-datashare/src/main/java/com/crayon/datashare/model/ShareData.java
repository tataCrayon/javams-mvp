package com.crayon.datashare.model;

import lombok.Data;

/**
 * 内存中的共享数据
 *
 * @author crayon
 * @version 1.0
 * @date 2025/5/14
 */
@Data
public class ShareData {


    private String id;

    /**
     * 数据更新时间戳
     */
    private Long lsn;


    /**
     * 数据
     */
    private Object data;

    /**
     * 数据版本
     */
    private int version;

    public ShareData(String id1, String initialValueForKey1, int version) {
        this.id = id1;
        this.data = initialValueForKey1;
        this.version = version;
        this.lsn = System.currentTimeMillis();
    }


    public void incrementVersion() {
        this.version++;
    }
}
