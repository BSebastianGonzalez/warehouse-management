package co.wm.warehouse.order;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import co.wm.warehouse.admin.Admin;
import co.wm.warehouse.product.Product;
import co.wm.warehouse.product.ProductNotFoundException;
import co.wm.warehouse.product.ProductRepository;
import co.wm.warehouse.stock.Stock;
import co.wm.warehouse.stock.StockRepository;
import co.wm.warehouse.movement.Movement;
import co.wm.warehouse.movement.MovementRepository;
import co.wm.warehouse.movement.MovementType;
import co.wm.warehouse.movement.InsufficientStockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final MovementRepository movementRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            StockRepository stockRepository,
            MovementRepository movementRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional
    public OrderResponse create(OrderRequest request, Admin administrator) {
        Set<Long> productIds = request.lines().stream().map(OrderLineRequest::productId).collect(Collectors.toSet());
        if (productIds.size() != request.lines().size()) {
            throw new InvalidOrderException("An order cannot contain duplicate products");
        }

        List<PreparedLine> preparedLines = request.lines().stream()
                .map(line -> prepareLine(line))
                .toList();
        boolean canDispatch = preparedLines.stream().allMatch(line -> line.missingQuantity() == 0);
        Order order = new Order(administrator, canDispatch ? OrderStatus.DISPATCHED : OrderStatus.CANCELLED);
        preparedLines.forEach(line -> order.addLine(new OrderLine(
                line.product(), line.requestedQuantity(), line.missingQuantity())));
        Order savedOrder = orderRepository.save(order);

        if (!canDispatch) {
            return OrderResponse.from(savedOrder);
        }

        for (int index = 0; index < preparedLines.size(); index++) {
            PreparedLine prepared = preparedLines.get(index);
            OrderLine orderLine = savedOrder.getLines().get(index);
            for (Allocation allocation : prepared.allocations()) {
                int updatedRows = stockRepository.decrementIfSufficient(
                        prepared.product().getId(), allocation.warehouse().getId(), allocation.quantity());
                if (updatedRows == 0) {
                    throw new InsufficientStockException(
                            prepared.product().getId(), allocation.warehouse().getId());
                }
                orderLine.addDispatchDetail(new DispatchDetail(allocation.warehouse(), allocation.quantity()));
                movementRepository.save(new Movement(
                        MovementType.OUTBOUND,
                        prepared.product(),
                        administrator,
                        allocation.warehouse(),
                        null,
                        allocation.quantity(),
                        "ORDER:" + savedOrder.getId()));
            }
        }
        return OrderResponse.from(orderRepository.save(savedOrder));
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id)));
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> findAll(
            OrderStatus status,
            Long administratorId,
            Instant from,
            Instant to,
            Pageable pageable) {
        Specification<Order> specification = Specification.unrestricted();
        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (administratorId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("administrator").get("id"), administratorId));
        }
        if (from != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        return orderRepository.findAll(specification, pageable).map(OrderSummaryResponse::from);
    }

    private PreparedLine prepareLine(OrderLineRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        List<Stock> stocks = stockRepository.findAllByProductIdWithWarehouse(product.getId());
        int remaining = request.requestedQuantity();
        List<Allocation> allocations = new java.util.ArrayList<>();
        for (Stock stock : stocks) {
            if (remaining == 0) {
                break;
            }
            int quantity = Math.min(remaining, stock.getQuantity());
            if (quantity > 0) {
                allocations.add(new Allocation(stock.getWarehouse(), quantity));
                remaining -= quantity;
            }
        }
        return new PreparedLine(product, request.requestedQuantity(), remaining, allocations);
    }

    private record PreparedLine(
            Product product,
            int requestedQuantity,
            int missingQuantity,
            List<Allocation> allocations) {
    }

    private record Allocation(co.wm.warehouse.warehouse.Warehouse warehouse, int quantity) {
    }
}
