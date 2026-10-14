package com.delivery.order.config;

import java.math.BigDecimal;
import com.delivery.order.model.Dish;
import com.delivery.order.repository.DishRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DishDataLoader {
    @Bean
    public CommandLineRunner seedDishes(DishRepository dishes) {
        return args -> {
            if (dishes.count() != 0) return;
            dishes.save(new Dish("House Burger", "Brioche bun, beef patty, cheese and lettuce",
                    new BigDecimal("39.90"), 10));
            dishes.save(new Dish("Vegetarian Pasta", "Penne with tomato, basil and vegetables",
                    new BigDecimal("35.00"), 30));
            dishes.save(new Dish("Margherita Pizza", "Mozzarella, tomato sauce and basil",
                    new BigDecimal("45.00"), 20));
            dishes.save(new Dish("Chicken Wrap", "Grilled chicken, lettuce and yogurt sauce",
                    new BigDecimal("29.90"), 25));
            dishes.save(new Dish("Vegan Bowl", "Rice, chickpeas, avocado and mixed vegetables",
                    new BigDecimal("37.50"), 15));
        };
    }
}
