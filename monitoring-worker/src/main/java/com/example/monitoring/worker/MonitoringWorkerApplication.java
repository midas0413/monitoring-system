package com.example.monitoring.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.example.monitoring")
@EnableJpaRepositories(basePackages = "com.example.monitoring.common.repo")
@EntityScan(basePackages = "com.example.monitoring.common.domain")
@ConfigurationPropertiesScan(basePackages = {"com.example.monitoring.worker", "com.example.monitoring.worker.alert"})
@EnableScheduling
public class MonitoringWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitoringWorkerApplication.class, args);
    }
}