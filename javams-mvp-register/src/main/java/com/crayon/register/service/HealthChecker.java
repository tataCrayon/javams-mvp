package com.crayon.register.service;

// 健康检查接口
public interface HealthChecker {
    // 检查服务实例是否健康
    boolean checkHealth(String ip, int port);

    // 获取检查类型名称
    String getType();
}