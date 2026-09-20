package co.wm.warehouse.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createsProductWithDefaultDiscontinuedValue() {
        ProductRequest request = new ProductRequest("SKU-1", "Paper", "unit", 10, null);
        Product savedProduct = new Product("SKU-1", "Paper", "unit", 10, false);
        when(productRepository.existsBySkuIgnoreCase("SKU-1")).thenReturn(false);
        when(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.create(request);

        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.discontinued()).isFalse();
        verify(productRepository).save(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void rejectsDuplicateSku() {
        ProductRequest request = new ProductRequest("SKU-1", "Paper", "unit", 10, false);
        when(productRepository.existsBySkuIgnoreCase("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateSkuException.class);
        verify(productRepository, never()).save(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void rejectsUnknownProductOnLookup() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void changesDiscontinuedStatusWithoutUpdatingOtherProductFields() {
        Product product = new Product("SKU-1", "Paper", "unit", 10, false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.changeDiscontinued(
                1L, new DiscontinuedRequest(true));

        assertThat(response.discontinued()).isTrue();
        assertThat(response.sku()).isEqualTo("SKU-1");
        assertThat(response.name()).isEqualTo("Paper");
    }
}
