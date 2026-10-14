package com.delivery.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "delivery.exchange";
    public static final String QUEUE = "reviews.queue";
    public static final String ROUTING_KEY = "reviews.new";

    @Bean
    public TopicExchange deliveryExchange() { return new TopicExchange(EXCHANGE, true, false); }

    @Bean
    public Queue reviewsQueue() { return QueueBuilder.durable(QUEUE).build(); }

    @Bean
    public Binding reviewBinding(Queue reviewsQueue, TopicExchange deliveryExchange) {
        return BindingBuilder.bind(reviewsQueue).to(deliveryExchange).with(ROUTING_KEY);
    }

}
