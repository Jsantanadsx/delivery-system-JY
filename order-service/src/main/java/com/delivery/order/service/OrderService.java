package com.delivery.order.service;

import com.delivery.order.api.OrderRequest;
import com.delivery.order.api.PaymentRequest;
import com.delivery.order.error.ApiException;
import com.delivery.order.model.CustomerOrder;
import com.delivery.order.model.Dish;
import com.delivery.order.repository.CustomerOrderRepository;
import com.delivery.order.repository.DishRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class OrderService {
    private final DishRepository dishes;
    private final CustomerOrderRepository orders;
    private final PaymentClient payments;

    public OrderService(DishRepository dishes, CustomerOrderRepository orders, PaymentClient payments) {
        this.dishes = dishes;
        this.orders = orders;
        this.payments = payments;
    }

    @Transactional
    public CustomerOrder placeOrder(OrderRequest request) {
        // Database write lock is held until transaction commits (or rolls back).
        Dish dish = dishes.findAndLockById(request.dishId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dish not found"));
        if (dish.getStock() < request.quantity()) {
            throw new ApiException(HttpStatus.CONFLICT, "Dish out of stock");
        }

        BigDecimal total = dish.getPrice().multiply(BigDecimal.valueOf(request.quantity()));
        dish.setStock(dish.getStock() - request.quantity());

        try {
            payments.charge(new PaymentRequest(total));
        } catch (RuntimeException ex) {
            // Unchecked exception intentionally aborts the JPA transaction.
            // A failed payment therefore never consumes stock or creates an order.
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment service unavailable");
        }

        return orders.save(new CustomerOrder(dish.getId(), request.quantity(), total));
    }

    @Transactional(readOnly = true)
    public CustomerOrder getOrder(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}
