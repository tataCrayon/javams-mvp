package com.crayon.register;

import com.crayon.register.model.Instance;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class HealthProtectionDemo {
    private float protectThreshold; // 健康保护阈值

    public HealthProtectionDemo(float protectThreshold) {
        this.protectThreshold = protectThreshold;
    }

    public static void main(String[] args) {
        // 创建实例列表
        List<Instance> instances = new ArrayList<>();
        instances.add(new Instance("192.168.1.1", 8080, true));
        instances.add(new Instance("192.168.1.2", 8080, false));
        instances.add(new Instance("192.168.1.3", 8080, false));

        // 设置阈值为 0.8
        HealthProtectionDemo demo = new HealthProtectionDemo(0.8f);

        // 获取实例
        List<Instance> result = demo.listInstances(instances);
        log.info("Result: " + result);
    }

    // 获取实例列表，模拟 Nacos 的 listInstances
    public List<Instance> listInstances(List<Instance> allInstances) {
        // 计算健康实例数
        int healthyCount = 0;
        for (Instance instance : allInstances) {
            if (instance.isHealthy()) {
                healthyCount++;
            }
        }

        // 计算健康比例
        float healthyRatio = allInstances.isEmpty() ? 0 : (float) healthyCount / allInstances.size();

        // 健康保护逻辑
        if (healthyRatio < protectThreshold && protectThreshold > 0) {
            log.info("Health ratio " + healthyRatio + " < threshold " + protectThreshold + ", return all instances");
            return new ArrayList<>(allInstances); // 返回所有实例
        }

        // 正常情况只返回健康实例
        List<Instance> healthyInstances = new ArrayList<>();
        for (Instance instance : allInstances) {
            if (instance.isHealthy()) {
                healthyInstances.add(instance);
            }
        }
        log.info("Health ratio " + healthyRatio + " >= threshold " + protectThreshold + ", return healthy instances");
        return healthyInstances;
    }
}