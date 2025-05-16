package com.crayon;

import com.crayon.datashare.client.SubscriberClient;
import com.crayon.datashare.model.ShareData;
import com.crayon.datashare.server.ShareDataServer;

/**
 * 简单数据共享中心演示
 *
 * @author crayon
 * @version 1.0
 * @date 2025/5/14
 */
public class Demo {

    public static void main(String[] args) {
        ShareDataServer shareDataServer = new ShareDataServer();

        // 模拟客户端1注册对 key1 的订阅
        SubscriberClient subscriberClient8080 = new SubscriberClient("127.0.0.1:8080");
        subscriberClient8080.subscribe("key1");
        SubscriberClient subscriberClient8081 = new SubscriberClient("127.0.0.1:8081");
        subscriberClient8081.subscribe("key1");

        // 模拟客户端2注册对 key2 的订阅
        subscriberClient8081.subscribe("key2");


        System.out.println("\n Setting data for key1...");
        shareDataServer.set("key1", new ShareData("id1", "Initial Value for key1", 1));
        System.out.println("\n Getting data for key1 by client1 (will re-register watcher)...");
        ShareData data1 = shareDataServer.get("key1", "client1_ip:port", true);
        System.out.println("Client1 got: " + data1);

        System.out.println("\n Setting data for key1 again (client1 should be notified)...");
        shareDataServer.set("key1", new ShareData("id1", "Updated Value for key1", 2));

        System.out.println("\n Setting data for key2...");
        shareDataServer.set("key2", new ShareData("id2", "Value for key2", 1));


        System.out.println("\n Client2 getting data for key1 (not subscribed initially, but sets a watch now)...");
        ShareData data1_by_client2 = shareDataServer.get("key1", "client2_ip:port", true);
        System.out.println("Client2 got (for key1): " + data1_by_client2);


        System.out.println("\n Simulating a read from a random slave for key1:");
        ShareData slaveData = shareDataServer.get("key1", null, false); // No re-register
        System.out.println("Read from slave for key1: " + slaveData);
    }

}
