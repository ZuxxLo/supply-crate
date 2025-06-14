package org.sc.msproducts.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sc.commonconfig.ApiResponse;
import org.sc.msproducts.dto.CategoryDTO;
import org.sc.msproducts.dto.ProductDTO;
import org.sc.msproducts.services.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        try {
            CategoryDTO createdCategory = categoryService.createCategory(categoryDTO);
            ApiResponse<CategoryDTO> response = new ApiResponse<>(createdCategory, "Category created successfully");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            ApiResponse<CategoryDTO> response = new ApiResponse<>(null, "Error creating category: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories(@RequestParam(defaultValue = "0") int page) {
        try {  int fixedSize = 10;
            Pageable pageable = PageRequest.of(page, fixedSize);

            Page<CategoryDTO> categoriePage = categoryService.getAllCategories( pageable);
            List<CategoryDTO> categories = categoriePage.getContent();

            ApiResponse<List<CategoryDTO>> response = new ApiResponse<>(categories, "Categories retrieved successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<List<CategoryDTO>> response = new ApiResponse<>(null, "Error retrieving categories: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryById(@PathVariable Long id) {
        try {
            CategoryDTO category = categoryService.getCategoryById(id);
            ApiResponse<CategoryDTO> response = new ApiResponse<>(category, "Category retrieved successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<CategoryDTO> response = new ApiResponse<>(null, "Error retrieving category: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTO categoryDTO) {
        try {
            CategoryDTO updatedCategory = categoryService.updateCategory(id, categoryDTO);
            ApiResponse<CategoryDTO> response = new ApiResponse<>(updatedCategory, "Category updated successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<CategoryDTO> response = new ApiResponse<>(null, "Error updating category: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            ApiResponse<Void> response = new ApiResponse<>(null, "Category deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<Void> response = new ApiResponse<>(null, "Error deleting category: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}