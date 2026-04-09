package com.actrade.activationrunner;

import com.actrade.activationrunner.config.ActradeApiProperties;
import com.actrade.activationrunner.config.DockerProperties;
import com.actrade.activationrunner.config.MinioProperties;
import com.actrade.activationrunner.config.RabbitTopologyProperties;
import com.actrade.activationrunner.config.RunnerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        RunnerProperties.class,
        RabbitTopologyProperties.class,
        ActradeApiProperties.class,
        MinioProperties.class,
        DockerProperties.class
})
public class ActivationRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivationRunnerApplication.class, args);
    }
}