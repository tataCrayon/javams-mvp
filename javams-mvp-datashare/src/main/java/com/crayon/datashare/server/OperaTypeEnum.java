package com.crayon.datashare.server;

/**
 * 操作类型枚举
 */
public enum OperaTypeEnum {

    GET("GET"),
    SET("SET"),

    CHANGE("CHANGE");


    private String type;

    OperaTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
