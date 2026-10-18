package com.utp.gajog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GajogApplication {

    public static void main(String[] args) {
        SpringApplication.run(GajogApplication.class, args);
    }

}