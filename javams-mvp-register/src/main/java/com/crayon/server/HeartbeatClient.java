package com.crayon.server;

import com.crayon.register.model.Instance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * 模拟服务实例使用定时任务发送心跳
 */
@Slf4j
@Component
public class HeartbeatClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private Instance instance;


    @PostConstruct
    public void init() {
        this.instance = new Instance();
        instance.setId(1);
        instance.setIp("127.0.0.1");
        instance.setPort(9081);
        instance.setServerName("user-service");
        instance.setStatus(Instance.Status.ONLINE);
    }

    /**
     * 定期发送心跳。每 30 秒发送一次
     */
    @Scheduled(fixedRate = 30000) //
    public void sendHeartbeat() {
        try {
            String url = "http://localhost:8080/register/heartbeat";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Instance> request = new HttpEntity<>(instance, headers);
            restTemplate.postForObject(url, request, Void.class);
            log.info("Heartbeat sent successfully: {}", instance);
        } catch (Exception e) {
            log.error("Failed to send heartbeat: {}", instance, e);
        }
    }
    /**
     * 启动时注册服务实例
     */
    @Scheduled(initialDelay = 0, fixedRate = Long.MAX_VALUE) // 启动时执行一次
    public void registerOnStartup() {
        try {
            String url = "http://localhost:8080/register/add";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Instance> request = new HttpEntity<>(instance, headers);
            restTemplate.postForObject(url, request, Void.class);
            log.info("Service registered on startup: {}", instance);
        } catch (Exception e) {
            log.error("Failed to register service: {}", instance, e);
        }
    }

}