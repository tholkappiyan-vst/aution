package com.example.aution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AutionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutionApplication.class, args);
    }

}