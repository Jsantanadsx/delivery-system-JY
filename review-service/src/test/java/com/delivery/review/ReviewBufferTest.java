package com.delivery.review;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.nio.charset.StandardCharsets;

import com.delivery.review.service.ReviewAggregate;
import com.delivery.review.service.ReviewBuffer;
import com.delivery.review.service.ReviewPersistence;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;

class ReviewBufferTest {
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void batchesRatingsByDishBeforePersistence() {
        ReviewPersistence persistence = mock(ReviewPersistence.class);
        ReviewBuffer buffer = new ReviewBuffer(persistence, JsonMapper.builder().build());
        MessageProperties properties = new MessageProperties();
        buffer.receive(new Message(json(1, 5).getBytes(StandardCharsets.UTF_8), properties));
        buffer.receive(new Message(json(1, 3).getBytes(StandardCharsets.UTF_8), properties));
        buffer.receive(new Message(json(2, 4).getBytes(StandardCharsets.UTF_8), properties));

        buffer.flush();

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(persistence).persist(captor.capture());
        Map<Long, ReviewAggregate> batch = captor.getValue();
        assertEquals(2, batch.size());
        assertEquals(8, batch.get(1L).ratingSum());
        assertEquals(2, batch.get(1L).count());
        assertEquals(4, batch.get(2L).ratingSum());
        assertEquals(1, batch.get(2L).count());
    }

    private String json(int id, int rating) {
        return "{\"dishId\":" + id + ",\"dishName\":\"Dish " + id
                + "\",\"rating\":" + rating + ",\"comment\":\"Good\"}";
    }
}
