package com.paradissaveurs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ParadisSaveursApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParadisSaveursApplication.class, args);
    }
}
