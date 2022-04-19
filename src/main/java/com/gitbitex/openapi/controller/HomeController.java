package com.gitbitex.openapi.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping(value = {"trade/*", "account/*"})
    public String test() {
        return "forward:/index.html";
    }
}


