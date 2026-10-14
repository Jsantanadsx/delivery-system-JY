package com.delivery.order.service;

import com.delivery.order.api.ReviewEvent;
import com.delivery.order.api.ReviewRequest;
import com.delivery.order.config.RabbitConfig;
import com.delivery.order.model.Dish;
import com.delivery.order.repository.DishRepository;
import com.delivery.order.error.ApiException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ReviewPublisher {
    private final DishRepository dishes;
    private final RabbitTemplate rabbitTemplate;
    private final JsonMapper jsonMapper;

    public ReviewPublisher(DishRepository dishes, RabbitTemplate rabbitTemplate, JsonMapper jsonMapper) {
        this.dishes = dishes;
        this.rabbitTemplate = rabbitTemplate;
        this.jsonMapper = jsonMapper;
    }

    public void publish(ReviewRequest request) {
        Dish dish = dishes.findById(request.dishId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dish not found"));
        ReviewEvent event = new ReviewEvent(dish.getId(), dish.getName(), request.rating(), request.comment());
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        // Send plain JSON without Java-specific type headers: services remain independent.
        rabbitTemplate.send(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY,
                new Message(jsonMapper.writeValueAsBytes(event), properties));
    }
}
