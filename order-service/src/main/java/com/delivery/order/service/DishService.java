package com.delivery.order.service;

import java.util.List;
import com.delivery.order.error.ApiException;
import com.delivery.order.model.Dish;
import com.delivery.order.repository.DishRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DishService {
    private final DishRepository dishes;
    public DishService(DishRepository dishes) { this.dishes = dishes; }

    @Transactional(readOnly = true)
    public List<Dish> findAll() { return dishes.findAll(); }

    @Transactional(readOnly = true)
    public Dish findById(Long id) {
        return dishes.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Dish not found"));
    }
}
