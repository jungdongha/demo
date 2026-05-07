package com.obigo.demodong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class DemoDongApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoDongApplication.class, args);
    }

}
