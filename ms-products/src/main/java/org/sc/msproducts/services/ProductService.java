package org.sc.msproducts.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.sc.msproducts.dto.ProductDTO;
import org.sc.msproducts.entities.*;

import org.sc.msproducts.repositories.BrandRepository;
import org.sc.msproducts.repositories.CategoryRepository;
import org.sc.msproducts.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
<<<<<<< HEAD
import java.math.RoundingMode;
import java.util.*;
=======
import java.util.List;
import java.util.Map;
>>>>>>> d589af2 (Add WebConfig and update configuration)
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.springframework.kafka.core.KafkaTemplate;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    @Value("${exchangerate.api.key}")
    private String apiKey;

    @Value("${exchangerate.api.url}")
    private String exchangeRateApiUrl;


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO, Long userId) {
        if (productRepository.existsBySku(productDTO.getSku())) {
            throw new IllegalArgumentException("SKU already exists");
        }

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        Brand brand = brandRepo.findById(productDTO.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        Product product = Product.builder()
                .name(productDTO.getName())
                .description(productDTO.getDescription())
                .sku(productDTO.getSku())
                .price(new Money(productDTO.getPrice(), "USD"))
                .category(category)
                .userId(userId)
                .brand(brand)
                .stockQty(productDTO.getStockQty())
                .status(ProductStatus.valueOf(productDTO.getStatus()))
                .build();

        product = productRepository.save(product);
        try {
            String event = objectMapper.writeValueAsString(product);
            System.out.println(event);
            System.out.println("-----------");
            kafkaTemplate.send("product-service-events", "ProductCreated", event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish ProductCreated event", e);
        }

        return mapToDTO(product, "USD");
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(String currency, Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(product -> mapToDTO(product, currency));
    }


    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id, String currency) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        return mapToDTO(product, currency);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO, Long userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (!isAdmin() && !product.getUserId().equals(userId)) {
            throw new SecurityException("You are not authorized to update this product");
        }
        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
        Brand brand = brandRepo.findById(productDTO.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found"));
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setSku(productDTO.getSku());
        product.setPrice(new Money(productDTO.getPrice(), "USD"));
        product.setCategory(category);
        product.setBrand(brand);

        product.setStockQty(productDTO.getStockQty());
        product.setStatus(ProductStatus.valueOf(productDTO.getStatus()));

        product = productRepository.save(product);
        try {
            String event = objectMapper.writeValueAsString(product);
            System.out.println("Serialized Product JSON event:\n" + event);
            kafkaTemplate.send("product-service-events", "ProductUpdated", event);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to publish ProductUpdated event", e);
        }


        return mapToDTO(product, "USD");
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Long userId = Long.valueOf(SecurityContextHolder.getContext().getAuthentication().getName());
        if (!isAdmin() && !product.getUserId().equals(userId)) {
            throw new SecurityException("You are not authorized to delete this product");
        }
        productRepository.deleteById(id);
        try {
            String event = objectMapper.writeValueAsString(Map.of("id", product.getId()));
            kafkaTemplate.send("product-service-events", "ProductDeleted", event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish ProductDeleted event", e);
        }
    }

    private ProductDTO mapToDTO(Product product, String targetCurrency) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setSku(product.getSku());
        dto.setPrice(product.getPrice().getAmount());
        dto.setCurrency(product.getPrice().getCurrency());
        dto.setBrandId(product.getBrand().getId());
        dto.setBrandName(product.getBrand().getName());
        dto.setCategoryName(product.getCategory().getName());
        dto.setCategoryId(product.getCategory().getId());
        dto.setUserId(product.getUserId());
        dto.setStockQty(product.getStockQty());
        dto.setStatus(product.getStatus().name());

        BigDecimal price = product.getPrice().getAmount();
        String currency = "USD";
        if (targetCurrency != null && !targetCurrency.equals("USD")) {
            try {
                BigDecimal exchangeRate = getExchangeRate("USD", targetCurrency);
                price = price.multiply(exchangeRate).setScale(2);
                currency = targetCurrency;
            } catch (Exception e) {
                // Fallback to USD if conversion fails
                System.err.println("Currency conversion failed: " + e.getMessage());
            }
        }
        dto.setPrice(price);
        dto.setCurrency(currency);

        return dto;
    }


    @Autowired
    private final RestTemplate restTemplate;

    private BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        String url = String.format("%s/v6/%s/latest/%s", exchangeRateApiUrl, apiKey, fromCurrency);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("conversion_rates")) {
            throw new IllegalStateException("Invalid response from exchange rate API");
        }
        Map<String, Double> rates = (Map<String, Double>) responseBody.get("conversion_rates");
        Double rate = rates.get(toCurrency);
        if (rate == null) {
            throw new IllegalArgumentException("Target currency not supported: " + toCurrency);
        }
        return BigDecimal.valueOf(rate);
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
    }


    @Autowired
    private BrandRepository brandRepo;

    public void importProductsFromCsv(InputStream inputStream, Long userId) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);
        Iterator<CSVRecord> iterator = csvParser.iterator();

        iterator.next(); // skip header

        Random random = new Random();
        int lineNumber = 1;

        while (iterator.hasNext()) {
            CSVRecord record = iterator.next();
            lineNumber++;

            System.out.println("Line " + lineNumber + ": " + record.toString());
            System.out.println("................");

            String sku = record.get(0).trim();
            String name = record.get(1).trim();
            Long brandId = Long.valueOf(record.get(3).trim());
            double price = Double.parseDouble(record.get(4).trim());
            String rawDescription = record.get(5).trim();
            String description = rawDescription.length() > 1000 ? rawDescription.substring(0, 1000) : rawDescription;
            if (productRepository.existsBySku(sku)) {
                continue;
            }

            Brand brand = brandRepo.findById(brandId)
                    .orElseThrow(() -> new RuntimeException("Brand not found: " + brandId));

            Long randomCategoryId = (long) 1; // or (random.nextInt(2) + 1);
            Category category = categoryRepository.findById(randomCategoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found: " + randomCategoryId));


            ProductDTO dto = new ProductDTO();
            dto.setName(name);
            dto.setDescription(description);
            dto.setSku(sku);
            dto.setCategoryId(category.getId());
            dto.setBrandId(brandId);
            dto.setPrice(BigDecimal.valueOf(price));
            dto.setStockQty(100);
            dto.setStatus(ProductStatus.ACTIVE.name());

            createProduct(dto, userId);

        }

        csvParser.close();
    }

}
