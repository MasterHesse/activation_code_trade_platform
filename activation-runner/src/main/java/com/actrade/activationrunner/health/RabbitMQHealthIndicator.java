package com.actrade.activationrunner.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 连接健康检查指示器
 *
 * <p>检查 RabbitMQ broker 连接是否可用。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitMQHealthIndicator implements HealthIndicator {

    private final ConnectionFactory connectionFactory;

    @Override
    public Health health() {
        try (Connection connection = connectionFactory.createConnection()) {
            if (connection.isOpen()) {
                return Health.up()
                        .withDetail("host", connectionFactory.getHost())
                        .withDetail("port", connectionFactory.getPort())
                        .withDetail("virtualHost", connectionFactory.getVirtualHost())
                        .withDetail("connection", "open")
                        .build();
            } else {
                return Health.down()
                        .withDetail("error", "Connection is not open")
                        .build();
            }
        } catch (Exception e) {
            log.warn("RabbitMQ health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("host", connectionFactory.getHost())
                    .withDetail("port", connectionFactory.getPort())
                    .build();
        }
    }
}
