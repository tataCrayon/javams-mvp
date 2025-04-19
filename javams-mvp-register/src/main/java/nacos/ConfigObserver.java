package nacos;

// 观察者接口 (Nacos 客户端 Listener)
public interface ConfigObserver {
    void onConfigChange(String dataId, String group, String newContent);
}
