package com.crayon.register.controller;

import com.crayon.register.model.Instance;
import com.crayon.register.service.RegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供对外接口，用于注册服务
 * @author crayon
 * @version 1.0
 * @date 2025/4/7
 */
@Slf4j
@RestController
@RequestMapping("/register")
public class RegistryController {

    @Autowired
    private RegistryService registryService;

    @PostMapping("/add")
    public void register(@RequestBody Instance instance) {
        registryService.register(instance.getServerName(), instance);
    }

    @PostMapping("/heartbeat")
    public void heartbeat(@RequestBody Instance instance) {
        registryService.heartbeat(instance);
    }

}
