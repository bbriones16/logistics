package com.logistics.system.inventory.service;

import com.logistics.system.common.exception.BusinessException;
import com.logistics.system.common.exception.ResourceNotFoundException;
import com.logistics.system.inventory.domain.Product;
import com.logistics.system.inventory.dto.ProductDto;
import com.logistics.system.inventory.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto.Response> findAll() {
        return productRepository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    public ProductDto.Response findById(Long id) {
        return toResponse(getProduct(id));
    }

    @Transactional
    public ProductDto.Response create(ProductDto.CreateRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new BusinessException("PRODUCT_DUPLICATE_SKU", "Product with SKU already exists: " + request.sku());
        }
        Product product = new Product();
        product.setSku(request.sku());
        product.setName(request.name());
        product.setDescription(request.description());
        product.setUnitOfMeasure(request.unitOfMeasure());
        product.setLowStockThreshold(request.lowStockThreshold());
        Product saved = productRepository.save(product);
        log.info("Product created: id={}, sku={}", saved.getId(), saved.getSku());
        return toResponse(saved);
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public Product getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product with SKU " + sku, 0L));
    }

    private ProductDto.Response toResponse(Product p) {
        return new ProductDto.Response(p.getId(), p.getSku(), p.getName(), p.getDescription(),
                p.getUnitOfMeasure(), p.getLowStockThreshold(), p.isActive());
    }
}
