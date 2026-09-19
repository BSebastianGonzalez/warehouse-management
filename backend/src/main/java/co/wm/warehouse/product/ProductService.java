package co.wm.warehouse.product;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String sku = request.sku().trim();
        ensureSkuIsAvailable(sku);
        Product product = new Product(
                sku,
                request.name().trim(),
                request.unitOfMeasure().trim(),
                request.minimumStock(),
                Boolean.TRUE.equals(request.discontinued()));
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        String sku = request.sku().trim();
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw new DuplicateSkuException(sku);
        }
        product.update(
                sku,
                request.name().trim(),
                request.unitOfMeasure().trim(),
                request.minimumStock(),
                Boolean.TRUE.equals(request.discontinued()));
        return ProductResponse.from(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private void ensureSkuIsAvailable(String sku) {
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateSkuException(sku);
        }
    }
}
