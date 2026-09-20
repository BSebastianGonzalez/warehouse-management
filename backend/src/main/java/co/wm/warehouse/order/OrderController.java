package co.wm.warehouse.order;

import co.wm.warehouse.admin.AdminService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }
}
