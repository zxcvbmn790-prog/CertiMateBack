package com.certimate.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CertimateManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertimateManagerApplication.class, args);
    }
}
