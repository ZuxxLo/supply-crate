package org.sc.msproducts.repositories;


import org.sc.msproducts.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySku(String sku);
}