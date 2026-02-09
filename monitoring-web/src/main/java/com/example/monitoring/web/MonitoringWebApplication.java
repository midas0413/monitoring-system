package com.example.monitoring.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.example.monitoring")
@EnableJpaRepositories(basePackages = "com.example.monitoring.common.repo")
@EntityScan(basePackages = "com.example.monitoring.common.domain")
public class MonitoringWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(MonitoringWebApplication.class, args);
    }
}