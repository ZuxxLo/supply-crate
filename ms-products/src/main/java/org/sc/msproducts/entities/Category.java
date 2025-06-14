package org.sc.msproducts.entities;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(name = "parent_category_id")
    private Long parentCategoryId;

 /*   @JsonIgnore
    @OneToMany(mappedBy = "category")
    private List<Product> products;*/
}