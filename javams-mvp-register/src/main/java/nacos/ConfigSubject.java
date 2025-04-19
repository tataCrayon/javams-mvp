package nacos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 被观察者 (Nacos 配置中心)
public class ConfigSubject {
    private Map<String, List<ConfigObserver>> observers = new HashMap<>();
    private Map<String, String> configStore = new HashMap<>(); // 模拟配置存储

    private String getConfigKey(String dataId, String group) {
        return dataId + "@" + group;
    }

    // 注册观察者 (客户端添加 Listener)
    public void addObserver(String dataId, String group, ConfigObserver observer) {
        String key = getConfigKey(dataId, group);
        observers.computeIfAbsent(key, k -> new ArrayList<>()).add(observer);
        System.out.println("Observer added for: " + key);
        // 首次添加时，可以考虑推送当前配置
        String currentContent = configStore.getOrDefault(key, null);
        if (currentContent != null) {
            observer.onConfigChange(dataId, group, currentContent);
        }
    }

    // 移除观察者
    public void removeObserver(String dataId, String group, ConfigObserver observer) {
        String key = getConfigKey(dataId, group);
        List<ConfigObserver> observerList = observers.get(key);
        if (observerList != null) {
            observerList.remove(observer);
            System.out.println("Observer removed for: " + key);
        }
    }

    // 发布配置 (模拟配置变更)
    public void publishConfig(String dataId, String group, String content) {
        String key = getConfigKey(dataId, group);
        System.out.println("Publishing config for " + key + ": " + content);
        configStore.put(key, content);
        notifyObservers(dataId, group, content);
    }

    // 通知观察者
    private void notifyObservers(String dataId, String group, String newContent) {
        String key = getConfigKey(dataId, group);
        List<ConfigObserver> observerList = observers.get(key);
        if (observerList != null && !observerList.isEmpty()) {
            System.out.println("Notifying " + observerList.size() + " observers for " + key);
            // 创建副本以防并发修改
            List<ConfigObserver> observersToNotify = new ArrayList<>(observerList);
            for (ConfigObserver observer : observersToNotify) {
                try {
                    observer.onConfigChange(dataId, group, newContent);
                } catch (Exception e) {
                    System.err.println("Error notifying observer: " + e.getMessage());
                }
            }
        }
    }
}