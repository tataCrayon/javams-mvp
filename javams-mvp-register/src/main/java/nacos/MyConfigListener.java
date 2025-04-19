package nacos;

// 具体观察者实现 (客户端应用中的监听器)
public class MyConfigListener implements ConfigObserver {
    private String listenerName;

    public MyConfigListener(String name) {
        this.listenerName = name;
    }

    @Override
    public void onConfigChange(String dataId, String group, String newContent) {
        System.out.println("[" + listenerName + "] Received config change: DataId=" + dataId + ", Group=" + group + ", Content=" + newContent);
        // 在这里处理配置变更逻辑，例如更新应用内部状态
    }
}