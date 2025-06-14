package org.sc.msproducts.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sc.commonconfig.ApiResponse;
import org.sc.msproducts.dto.BrandDTO;
import org.sc.msproducts.dto.CategoryDTO;
import org.sc.msproducts.dto.ProductDTO;
import org.sc.msproducts.services.BrandService;
import org.sc.msproducts.services.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandDTO>> createBrand(@Valid @RequestBody BrandDTO brandDTO) {
        try {
            BrandDTO createdBrand = brandService.createBrand(brandDTO);
            ApiResponse<BrandDTO> response = new ApiResponse<>(createdBrand, "Brand created successfully");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            ApiResponse<BrandDTO> response = new ApiResponse<>(null, "Error creating brand: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandDTO>>> getAllBrands(@RequestParam(defaultValue = "0") int page) {
        try {
            int fixedSize = 10;
            Pageable pageable = PageRequest.of(page, fixedSize);

            Page<BrandDTO> brandPage = brandService.getAllBrands(pageable);
            List<BrandDTO> brands = brandPage.getContent();

            ApiResponse<List<BrandDTO>> response = new ApiResponse<>(brands, "Brands retrieved successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<List<BrandDTO>> response = new ApiResponse<>(null, "Error retrieving brands: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandDTO>> getBrandById(@PathVariable Long id) {
        try {
            BrandDTO brand = brandService.getBrandById(id);
            ApiResponse<BrandDTO> response = new ApiResponse<>(brand, "Brand retrieved successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<BrandDTO> response = new ApiResponse<>(null, "Error retrieving brand: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BrandDTO>> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandDTO brandDTO) {
        try {
            BrandDTO updatedBrand = brandService.updateBrand(id, brandDTO);
            ApiResponse<BrandDTO> response = new ApiResponse<>(updatedBrand, "Brand updated successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<BrandDTO> response = new ApiResponse<>(null, "Error updating brand: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long id) {
        try {
            brandService.deleteBrand(id);
            ApiResponse<Void> response = new ApiResponse<>(null, "Brand deleted successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<Void> response = new ApiResponse<>(null, "Error deleting brand: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @Operation(
            summary = "Import brands from a CSV file",
            description = "Uploads a CSV file to import brands. Requires ADMIN role.",
            security = @SecurityRequirement(name = "bearerAuth")
    )

    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> importBrandsFromCsv(
            @Parameter(description = "CSV file containing brand data", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        try {
            brandService.importBrandsFromCsv(file.getInputStream());
            ApiResponse<String> response = new ApiResponse<>(null, "Brands imported successfully");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            ApiResponse<String> response = new ApiResponse<>(null, "Error importing brands: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }
}
