package com.delivery.order.api;

import com.delivery.order.service.ReviewPublisher;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {
    private final ReviewPublisher publisher;
    public ReviewController(ReviewPublisher publisher) { this.publisher = publisher; }

    @PostMapping("/reviews")
    public ResponseEntity<Void> create(@Valid @RequestBody ReviewRequest request) {
        publisher.publish(request);
        return ResponseEntity.accepted().build();
    }
}
