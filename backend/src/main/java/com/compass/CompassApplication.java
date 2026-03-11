package com.compass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // 이거 꼭 있어야 createdAt, updatedAt이 자동으로 찍혀!
@SpringBootApplication
public class CompassApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompassApplication.class, args);
    }
}