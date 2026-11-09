package com.zjgsu.zyw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableFeignClients(basePackages = "com.zjgsu.zyw.client")
@SpringBootApplication
public class LibraryBorrowApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryBorrowApplication.class, args);
    }

}
