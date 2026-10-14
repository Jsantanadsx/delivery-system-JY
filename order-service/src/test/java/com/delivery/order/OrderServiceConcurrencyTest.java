package com.delivery.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.delivery.order.api.OrderRequest;
import com.delivery.order.api.PaymentResponse;
import com.delivery.order.error.ApiException;
import com.delivery.order.model.Dish;
import com.delivery.order.repository.CustomerOrderRepository;
import com.delivery.order.repository.DishRepository;
import com.delivery.order.service.OrderService;
import com.delivery.order.service.PaymentClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:race;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=120000",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key=test"
})
class OrderServiceConcurrencyTest {
    @Autowired private OrderService service;
    @Autowired private DishRepository dishes;
    @Autowired private CustomerOrderRepository orders;
    @MockitoBean private PaymentClient payments;

    @BeforeEach
    void resetStock() {
        orders.deleteAll();
        Dish promotion = dishes.findById(1L).orElseThrow();
        promotion.setStock(10);
        dishes.saveAndFlush(promotion);
        reset(payments);
    }

    @Test
    void fiftyConcurrentRequestsCreateExactlyTenOrders() throws Exception {
        when(payments.charge(any())).thenReturn(new PaymentResponse("APPROVED", 8081));
        int requestCount = 50;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < requestCount; i++) {
                Callable<Integer> task = () -> {
                    ready.countDown();
                    start.await();
                    try {
                        service.placeOrder(new OrderRequest(1L, 1));
                        return 201;
                    } catch (ApiException ex) {
                        return ex.getStatus().value();
                    }
                };
                futures.add(executor.submit(task));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int created = 0;
            int conflicts = 0;
            for (Future<Integer> future : futures) {
                int status = future.get(120, TimeUnit.SECONDS);
                if (status == 201) created++;
                if (status == 409) conflicts++;
            }
            assertEquals(10, created);
            assertEquals(40, conflicts);
            assertEquals(10L, orders.count());
            assertEquals(0, dishes.findById(1L).orElseThrow().getStock());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedPaymentRollsBackStockAndOrder() {
        when(payments.charge(any())).thenThrow(new RestClientException("Payment down"));
        ApiException ex = assertThrows(ApiException.class,
                () -> service.placeOrder(new OrderRequest(1L, 1)));
        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertEquals(10, dishes.findById(1L).orElseThrow().getStock());
        assertEquals(0, orders.count());
    }
}
