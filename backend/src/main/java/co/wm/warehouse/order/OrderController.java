package co.wm.warehouse.order;

import co.wm.warehouse.admin.AdminService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AdminService adminService;

    public OrderController(OrderService orderService, AdminService adminService) {
        this.orderService = orderService;
        this.adminService = adminService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request, HttpSession session) {
        return orderService.create(request, adminService.requireAuthenticated(session));
    }

    @GetMapping
    public Page<OrderSummaryResponse> findAll(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long administratorId,
            @RequestParam(required = false) java.time.Instant from,
            @RequestParam(required = false) java.time.Instant to,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpSession session) {
        adminService.requireAuthenticated(session);
        return orderService.findAll(status, administratorId, from, to, pageable);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id, HttpSession session) {
        adminService.requireAuthenticated(session);
        return orderService.findById(id);
    }
}
