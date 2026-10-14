package com.delivery.review.service;

import java.util.Map;
import com.delivery.review.model.ReviewSummary;
import com.delivery.review.repository.ReviewSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewPersistence {
    private final ReviewSummaryRepository repository;

    public ReviewPersistence(ReviewSummaryRepository repository) { this.repository = repository; }

    @Transactional
    public void persist(Map<Long, ReviewAggregate> batch) {
        for (Map.Entry<Long, ReviewAggregate> entry : batch.entrySet()) {
            Long id = entry.getKey();
            ReviewAggregate aggregate = entry.getValue();
            ReviewSummary summary = repository.findById(id)
                    .orElseGet(() -> new ReviewSummary(id, aggregate.dishName()));
            summary.merge(aggregate.dishName(), aggregate.ratingSum(), aggregate.count());
            repository.save(summary);
        }
    }
}
