package com.delivery.review.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "review_summaries")
public class ReviewSummary {
    @Id
    private Long dishId;

    @Column(nullable = false)
    private String dishName;

    @Column(nullable = false)
    private long ratingSum;

    @Column(nullable = false)
    private long ratingCount;

    protected ReviewSummary() { }

    public ReviewSummary(Long dishId, String dishName) {
        this.dishId = dishId;
        this.dishName = dishName;
    }

    public Long getDishId() { return dishId; }
    public String getDishName() { return dishName; }
    public long getRatingSum() { return ratingSum; }
    public long getRatingCount() { return ratingCount; }

    public void merge(String name, long sum, long count) {
        this.dishName = name;
        this.ratingSum += sum;
        this.ratingCount += count;
    }
}
