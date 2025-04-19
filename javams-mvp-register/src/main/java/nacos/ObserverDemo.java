package nacos;


public class ObserverDemo {
    public static void main(String[] args) {
        ConfigSubject configCenter = new ConfigSubject();

        MyConfigListener listener1 = new MyConfigListener("App1-Listener");
        MyConfigListener listener2 = new MyConfigListener("App2-Listener");

        // App1 和 App2 都监听同一个配置
        configCenter.addObserver("app.properties", "DEFAULT_GROUP", listener1);
        configCenter.addObserver("app.properties", "DEFAULT_GROUP", listener2);

        System.out.println("\n--- Publishing first config ---");
        configCenter.publishConfig("app.properties", "DEFAULT_GROUP", "database.url=jdbc:mysql://localhost:3306/mydb");

        System.out.println("\n--- Publishing updated config ---");
        configCenter.publishConfig("app.properties", "DEFAULT_GROUP", "database.url=jdbc:mysql://remote:3306/prod_db");

        System.out.println("\n--- App1 stops listening ---");
        configCenter.removeObserver("app.properties", "DEFAULT_GROUP", listener1);

        System.out.println("\n--- Publishing final config ---");
        configCenter.publishConfig("app.properties", "DEFAULT_GROUP", "database.url=jdbc:mysql://new_remote:3306/final_db");
    }
}