package com.delivery.order.api;

import jakarta.validation.constraints.NotBlank;

public record AssistantRequest(@NotBlank(message = "Question is required") String question) { }
