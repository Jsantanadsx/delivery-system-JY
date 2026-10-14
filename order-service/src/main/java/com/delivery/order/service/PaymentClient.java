package com.delivery.order.service;

import com.delivery.order.api.PaymentRequest;
import com.delivery.order.api.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

@Component
public class PaymentClient {
    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    private final RestTemplate paymentRestTemplate;

    public PaymentClient(RestTemplate paymentRestTemplate) {
        this.paymentRestTemplate = paymentRestTemplate;
    }

    // 1 initial attempt + up to 4 retries, with exponential backoff and jitter.
    // Separate Spring bean: internal self-invocation would bypass the retry proxy.
    @Retryable(includes = RestClientException.class,
            maxRetries = 4, delay = 100, multiplier = 2.0, jitter = 30, maxDelay = 800)
    public PaymentResponse charge(PaymentRequest request) {
        log.info("Calling PAYMENT-SERVICE through Eureka/LoadBalancer, amount={}", request.amount());
        PaymentResponse response = paymentRestTemplate.postForObject(
                "http://PAYMENT-SERVICE/payments", request, PaymentResponse.class);
        if (response == null || !"APPROVED".equals(response.status())) {
            throw new IllegalStateException("Payment not approved");
        }
        log.info("Payment approved on instance port={}", response.instance());
        return response;
    }
}
