package com.masterhesse.activation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ACTIVATION_TASK_EXCHANGE = "activation.task.exchange";
    public static final String ACTIVATION_TASK_ROUTING_KEY = "activation.task";
    public static final String ACTIVATION_TASK_QUEUE = "activation.task.queue";

    public static final String ACTIVATION_TASK_RETRY_EXCHANGE = "activation.task.retry.exchange";
    public static final String ACTIVATION_TASK_RETRY_ROUTING_KEY = "activation.task.retry";
    public static final String ACTIVATION_TASK_RETRY_QUEUE = "activation.task.retry.queue";

    @Bean
    public DirectExchange activationTaskExchange() {
        return new DirectExchange(ACTIVATION_TASK_EXCHANGE, true, false);
    }

    @Bean
    public Queue activationTaskQueue() {
        return QueueBuilder.durable(ACTIVATION_TASK_QUEUE).build();
    }

    @Bean
    public Binding activationTaskBinding() {
        return BindingBuilder.bind(activationTaskQueue())
                .to(activationTaskExchange())
                .with(ACTIVATION_TASK_ROUTING_KEY);
    }

    @Bean
    public DirectExchange activationTaskRetryExchange() {
        return new DirectExchange(ACTIVATION_TASK_RETRY_EXCHANGE, true, false);
    }

    @Bean
    public Queue activationTaskRetryQueue() {
        return QueueBuilder.durable(ACTIVATION_TASK_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", ACTIVATION_TASK_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", ACTIVATION_TASK_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding activationTaskRetryBinding() {
        return BindingBuilder.bind(activationTaskRetryQueue())
                .to(activationTaskRetryExchange())
                .with(ACTIVATION_TASK_RETRY_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}