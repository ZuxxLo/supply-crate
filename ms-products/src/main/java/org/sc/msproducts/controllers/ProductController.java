package org.sc.msproducts.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sc.commonconfig.ApiResponse;
import org.sc.msproducts.dto.ProductDTO;
import org.sc.msproducts.services.ProductService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@Valid @RequestBody ProductDTO productDTO) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            System.out.println(userId);
            System.out.println("222222222222222222");
            if (userId.isEmpty()) {
                throw new IllegalArgumentException("User ID is missing from Security Context");
            }
            System.out.println(userId);
            System.out.println("---------sursessq");
            ProductDTO createdProduct = productService.createProduct(productDTO, Long.valueOf(userId));
            ApiResponse<ProductDTO> response = new ApiResponse<>(createdProduct, "Product created successfully");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            ApiResponse<ProductDTO> response = new ApiResponse<>(null, "Error creating product: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getAllProducts(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "USD") String currency) {
        try {
            int fixedSize = 10;
            Pageable pageable = PageRequest.of(page, fixedSize);

            Page<ProductDTO> productPage = productService.getAllProducts(currency, pageable);
            List<ProductDTO> products = productPage.getContent();
            ApiResponse<List<ProductDTO>> response = new ApiResponse<>(products, "Products retrieved successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<List<ProductDTO>> response = new ApiResponse<>(null, "Error retrieving products: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> getProductById(@PathVariable Long id, @RequestParam(defaultValue = "USD") String currency) {
        try {
            ProductDTO product = productService.getProductById(id, currency);
            ApiResponse<ProductDTO> response = new ApiResponse<>(product, "Product retrieved successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<ProductDTO> response = new ApiResponse<>(null, "Error retrieving product: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO productDTO) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (userId.isEmpty()) {
                throw new IllegalArgumentException("User ID is missing from Security Context");
            }
            ProductDTO updatedProduct = productService.updateProduct(id, productDTO, Long.valueOf(userId));
            ApiResponse<ProductDTO> response = new ApiResponse<>(updatedProduct, "Product updated successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            ApiResponse<ProductDTO> response = new ApiResponse<>(null, "Error updating product: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            ApiResponse<ProductDTO> response = new ApiResponse<>(null, "Unauthorized: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            ApiResponse<ProductDTO> response = new ApiResponse<>(null, "Unexpected error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            ApiResponse<Void> response = new ApiResponse<>(null, "Product deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            ApiResponse<Void> response = new ApiResponse<>(null, "Error deleting product: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } catch (SecurityException e) {
            ApiResponse<Void> response = new ApiResponse<>(null, "Unauthorized: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            ApiResponse<Void> response = new ApiResponse<>(null, "Unexpected error: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/import-csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> importBrandsFromCsv(
            @Parameter(description = "CSV file containing brand data", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        try {
            String userId = SecurityContextHolder.getContext().getAuthentication().getName();

            productService.importProductsFromCsv(file.getInputStream(), Long.valueOf(userId));
            ApiResponse<String> response = new ApiResponse<>(null, "Products imported successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>(null, "Error importing products: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}