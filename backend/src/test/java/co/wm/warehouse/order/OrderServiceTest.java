package co.wm.warehouse.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import co.wm.warehouse.admin.Admin;
import co.wm.warehouse.movement.Movement;
import co.wm.warehouse.movement.MovementRepository;
import co.wm.warehouse.product.Product;
import co.wm.warehouse.product.ProductRepository;
import co.wm.warehouse.stock.Stock;
import co.wm.warehouse.stock.StockRepository;
import co.wm.warehouse.warehouse.Warehouse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private MovementRepository movementRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void cancelsOrderWithoutChangingStockWhenLineCannotBeCompleted() {
        Product product = product(1L);
        Admin admin = org.mockito.Mockito.mock(Admin.class);
        Warehouse warehouse = warehouse(1L);
        Stock availableStock = stock(product, warehouse, 2);
        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(product));
        when(stockRepository.findAllByProductIdWithWarehouse(1L)).thenReturn(List.of(availableStock));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(
                new OrderRequest(List.of(new OrderLineRequest(1L, 5))), admin);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(response.lines().get(0).missingQuantity()).isEqualTo(3);
        assertThat(response.lines().get(0).dispatchDetails()).isEmpty();
        verify(stockRepository, never()).decrementIfSufficient(any(), any(), any());
        verify(movementRepository, never()).save(any(Movement.class));
    }

    @Test
    void dispatchesOneLineAcrossMultipleWarehouses() {
        Product product = product(1L);
        Admin admin = org.mockito.Mockito.mock(Admin.class);
        Warehouse first = warehouse(1L);
        Warehouse second = warehouse(2L);
        when(first.getId()).thenReturn(1L);
        when(second.getId()).thenReturn(2L);
        Stock firstStock = stock(product, first, 2);
        Stock secondStock = stock(product, second, 3);
        when(productRepository.findById(1L)).thenReturn(java.util.Optional.of(product));
        when(stockRepository.findAllByProductIdWithWarehouse(1L)).thenReturn(List.of(firstStock, secondStock));
        when(stockRepository.decrementIfSufficient(1L, 1L, 2)).thenReturn(1);
        when(stockRepository.decrementIfSufficient(1L, 2L, 3)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(
                new OrderRequest(List.of(new OrderLineRequest(1L, 5))), admin);

        assertThat(response.status()).isEqualTo(OrderStatus.DISPATCHED);
        assertThat(response.lines().get(0).missingQuantity()).isZero();
        assertThat(response.lines().get(0).dispatchDetails()).hasSize(2);
        verify(movementRepository, org.mockito.Mockito.times(2)).save(any(Movement.class));
    }

    @Test
    void listsOrdersWithoutFiltersUsingAnUnrestrictedSpecification() {
        Pageable pageable = Pageable.ofSize(20);
        when(orderRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        orderService.findAll(null, null, null, null, pageable);

        verify(orderRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class),
                org.mockito.Mockito.eq(pageable));
    }

    private Product product(Long id) {
        Product product = org.mockito.Mockito.mock(Product.class);
        when(product.getId()).thenReturn(id);
        return product;
    }

    private Warehouse warehouse(Long id) {
        return org.mockito.Mockito.mock(Warehouse.class);
    }

    private Stock stock(Product product, Warehouse warehouse, int quantity) {
        Stock stock = org.mockito.Mockito.mock(Stock.class);
        when(stock.getWarehouse()).thenReturn(warehouse);
        when(stock.getQuantity()).thenReturn(quantity);
        return stock;
    }
}
