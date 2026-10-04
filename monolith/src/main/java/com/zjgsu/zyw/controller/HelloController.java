package com.zjgsu.zyw.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api")
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String say(){
        return "<h1>hello</h1>";
    }
}
