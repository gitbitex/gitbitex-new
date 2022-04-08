package com.gitbitex.module.openapi.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {

    @GetMapping("/configs")
    public Map<String, Object> getConfigs() {
        return null;
    }

}
