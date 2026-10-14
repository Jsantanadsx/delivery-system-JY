package com.delivery.payment;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final int serverPort;

    public PaymentController(@Value("${server.port:8081}") int serverPort) {
        this.serverPort = serverPort;
    }

    @PostMapping("/payments")
    public ResponseEntity<Map<String, Object>> pay(@RequestBody PaymentRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Amount must be positive"));
        }
        // Randomly fail in about 50% of requests to exercise load balancing and retries.
        if (ThreadLocalRandom.current().nextBoolean()) {
            log.warn("Payment rejected (simulated error) by instance {} | amount={}", serverPort, request.amount());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Simulated payment provider failure", "instance", serverPort));
        }
        log.info("Payment approved by instance {} | amount={}", serverPort, request.amount());
        return ResponseEntity.ok(Map.of("status", "APPROVED", "instance", serverPort));
    }

    public record PaymentRequest(BigDecimal amount) { }
}
