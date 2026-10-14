package com.delivery.review.api;

public record ReviewEvent(Long dishId, String dishName, Integer rating, String comment) { }
