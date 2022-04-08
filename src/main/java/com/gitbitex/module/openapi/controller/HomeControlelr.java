package com.gitbitex.module.openapi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeControlelr {
    @GetMapping(value = {"/trade/BTC-USDT", "account/*"})
    public String test() {
        return "forward:/index.html";
    }
}


