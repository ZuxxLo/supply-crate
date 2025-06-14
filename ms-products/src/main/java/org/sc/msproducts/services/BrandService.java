package org.sc.msproducts.services;

import org.sc.msproducts.dto.BrandDTO;
import org.sc.msproducts.entities.Brand;
import org.sc.msproducts.repositories.BrandRepository;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

@Service
public class BrandService {

    @Autowired
    private BrandRepository brandRepository;

    public void importBrandsFromCsv(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        reader.readLine();

        Set<String> seen = new HashSet<>();

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            String brandName = parts[1].trim();

            if (!seen.contains(brandName)) {
                seen.add(brandName);
                if (brandRepository.findByName(brandName).isEmpty()) {
                    brandRepository.save(new Brand(null, brandName));
                }
            }
        }
    }

    @Transactional
    public BrandDTO createBrand(BrandDTO brandDTO) {
        Brand brand = new Brand();
        brand.setName(brandDTO.getName());

        brand = brandRepository.save(brand);
        return mapToDTO(brand);
    }

    @Transactional(readOnly = true)
    public Page<BrandDTO> getAllBrands(Pageable pageable) {
        return brandRepository.findAll(pageable)
                .map(this::mapToDTO);
    }

    @Transactional(readOnly = true)
    public BrandDTO getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        return mapToDTO(brand);
    }

    @Transactional
    public BrandDTO updateBrand(Long id, BrandDTO brandDTO) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));

        brand.setName(brandDTO.getName());

        brand = brandRepository.save(brand);
        return mapToDTO(brand);
    }

    @Transactional
    public void deleteBrand(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new IllegalArgumentException("Brand not found");
        }
        brandRepository.deleteById(id);
    }

    private BrandDTO mapToDTO(Brand brand) {
        BrandDTO dto = new BrandDTO();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        return dto;
    }
}