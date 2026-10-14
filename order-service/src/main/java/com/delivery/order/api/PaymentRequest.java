package com.delivery.order.api;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) { }
