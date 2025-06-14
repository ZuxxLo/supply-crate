package org.sc.msproducts.dto;


import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price must be positive")
    private BigDecimal price;

    private String currency;
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    private String categoryName;

    @NotNull(message = "Brand ID is required")
    private Long brandId;
    private String brandName;

    private Long userId;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQty;

    @NotBlank(message = "Status is required")
    private String status;
}