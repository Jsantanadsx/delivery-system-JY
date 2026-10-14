package com.delivery.order.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull(message = "Dish id is required") Long dishId,
        @NotNull(message = "Rating is required") @Min(1) @Max(5) Integer rating,
        String comment) { }
