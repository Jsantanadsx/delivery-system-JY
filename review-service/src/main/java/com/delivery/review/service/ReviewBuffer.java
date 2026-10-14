package com.delivery.review.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.delivery.review.api.ReviewEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReviewBuffer {
    private static final Logger log = LoggerFactory.getLogger(ReviewBuffer.class);
    private final ConcurrentHashMap<Long, ReviewAggregate> pending = new ConcurrentHashMap<>();
    private final ReviewPersistence persistence;
    private final JsonMapper jsonMapper;

    public ReviewBuffer(ReviewPersistence persistence, JsonMapper jsonMapper) {
        this.persistence = persistence;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = "reviews.queue")
    public void receive(Message message) {
        ReviewEvent event;
        try {
            event = jsonMapper.readValue(message.getBody(), ReviewEvent.class);
        } catch (RuntimeException ex) {
            log.warn("Ignored malformed JSON review message", ex);
            return;
        }
        if (event.dishId() == null || event.dishName() == null
                || event.rating() == null || event.rating() < 1 || event.rating() > 5) {
            log.warn("Ignored malformed review event: {}", event);
            return;
        }
        ReviewAggregate one = new ReviewAggregate(event.dishName(), event.rating(), 1);
        pending.merge(event.dishId(), one, ReviewAggregate::merge);
    }

    @Scheduled(fixedDelay = 5000)
    public void flush() {
        Map<Long, ReviewAggregate> batch = new HashMap<>();
        // Atomic per-key remove: never lose a concurrently arriving rating.
        pending.forEach((id, value) -> pending.computeIfPresent(id, (key, current) -> {
            batch.put(key, current);
            return null;
        }));
        if (batch.isEmpty()) return;

        try {
            persistence.persist(batch);
            log.info("Flushed {} dish aggregates to H2", batch.size());
        } catch (RuntimeException ex) {
            // Persistence rolls back on failure; restore buffered data for next flush.
            batch.forEach((id, item) -> pending.merge(id, item, ReviewAggregate::merge));
            log.error("Review flush failed; buffered items restored", ex);
        }
    }
}
