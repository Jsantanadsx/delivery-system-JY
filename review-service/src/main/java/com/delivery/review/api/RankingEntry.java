package com.delivery.review.api;

public record RankingEntry(Long dishId, String dishName, double average, long count) { }
