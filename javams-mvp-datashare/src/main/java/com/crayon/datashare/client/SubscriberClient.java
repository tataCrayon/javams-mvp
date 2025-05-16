package com.crayon.datashare.client;

import com.crayon.datashare.model.ShareData;
import com.crayon.datashare.server.ShareDataServer;

/**
 * @author crayon
 * @version 1.0
 * @date 2025/5/16
 */
public class SubscriberClient {


    private String ipPort;
    private ShareDataServer shareDataServer = new ShareDataServer();

    public SubscriberClient(String ipPort) {
        this.ipPort = ipPort;
    }

    public void subscribe(String key) {
        shareDataServer.register(key, ipPort);
    }

    public ShareData get(String key, String ipPort) {
        return shareDataServer.get(key, ipPort, true);
    }

}
