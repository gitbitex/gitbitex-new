package com.gitbitex.openapi.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CodeController {

    @PostMapping("/codes")
    public void getCode() {
        throw new RuntimeException("123456");
    }

}
