package com.zjgsu.zyw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MonolithServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonolithServerApplication.class, args);
    }

}
