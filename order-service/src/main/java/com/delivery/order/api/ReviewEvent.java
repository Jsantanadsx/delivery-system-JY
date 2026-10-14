package com.delivery.order.api;

public record ReviewEvent(Long dishId, String dishName, Integer rating, String comment) { }
