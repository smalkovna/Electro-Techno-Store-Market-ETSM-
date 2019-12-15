package com.etsm.ETSM.Repositories;

import com.etsm.ETSM.Models.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Репозиторий хранящий в себе весь список товаров (также занимается выборкой)

public interface ProductRepository extends JpaRepository<Product, Long> {
    public Optional<Product> findByName(String name);
}
