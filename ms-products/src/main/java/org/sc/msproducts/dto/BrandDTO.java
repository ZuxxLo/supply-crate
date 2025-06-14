package org.sc.msproducts.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data

public class BrandDTO {
    private Long id;

    @NotBlank(message = "Name is required")
    private String name;

}
