package com.crayon.register.service;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class HealthCheckerLoader {
    private static final List<HealthChecker> checkers = new ArrayList<>();

    static {
        // 使用 Java SPI 加载所有实现
        ServiceLoader<HealthChecker> loader = ServiceLoader.load(HealthChecker.class);
        for (HealthChecker checker : loader) {
            checkers.add(checker);
        }
    }

    // 根据类型获取健康检查器
    public static HealthChecker getChecker(String type) {
        for (HealthChecker checker : checkers) {
            if (checker.getType().equalsIgnoreCase(type)) {
                return checker;
            }
        }
        throw new IllegalArgumentException("No HealthChecker found for type: " + type);
    }

    // 获取所有检查器
    public static List<HealthChecker> getAllCheckers() {
        return new ArrayList<>(checkers);
    }
}