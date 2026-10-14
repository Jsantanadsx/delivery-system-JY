package com.delivery.review.service;

/** Immutable snapshot of the in-memory aggregation for a single dish. */
public record ReviewAggregate(String dishName, long ratingSum, long count) {
    public ReviewAggregate merge(ReviewAggregate other) {
        return new ReviewAggregate(other.dishName(), ratingSum + other.ratingSum(), count + other.count());
    }
}
