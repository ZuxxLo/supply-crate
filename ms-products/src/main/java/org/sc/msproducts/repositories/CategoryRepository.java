package org.sc.msproducts.repositories;


import org.sc.msproducts.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}