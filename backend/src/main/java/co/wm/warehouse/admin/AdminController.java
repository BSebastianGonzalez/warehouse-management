package co.wm.warehouse.admin;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminResponse register(@Valid @RequestBody AdminRequest request) {
        return adminService.register(request);
    }

    @PostMapping("/login")
    public AdminResponse login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return adminService.login(request, session);
    }

    @GetMapping("/administrators")
    public List<AdminResponse> findAll(HttpSession session) {
        adminService.requireAuthenticated(session);
        return adminService.findAll();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        adminService.logout(session);
    }
}
