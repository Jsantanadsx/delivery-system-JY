package com.delivery.order.api;

import java.util.List;
import com.delivery.order.model.Dish;
import com.delivery.order.service.DishService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dishes")
public class DishController {
    private final DishService dishes;
    public DishController(DishService dishes) { this.dishes = dishes; }

    @GetMapping
    public List<Dish> all() { return dishes.findAll(); }

    @GetMapping("/{id}")
    public Dish one(@PathVariable Long id) { return dishes.findById(id); }
}
