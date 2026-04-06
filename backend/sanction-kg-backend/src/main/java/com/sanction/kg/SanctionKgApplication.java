package com.sanction.kg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SanctionKgApplication {

    public static void main(String[] args) {
        SpringApplication.run(SanctionKgApplication.class, args);
    }
}
