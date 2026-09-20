package co.wm.warehouse.admin;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AdminResponse register(AdminRequest request) {
        if (adminRepository.count() > 0) {
            throw new DuplicateAdminUsernameException("registration is closed after the first administrator");
        }
        String username = request.username().trim();
        if (adminRepository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateAdminUsernameException(username);
        }
        Admin admin = new Admin(username, passwordEncoder.encode(request.password()), request.name().trim());
        return AdminResponse.from(adminRepository.save(admin));
    }

    @Transactional(readOnly = true)
    public AdminResponse login(LoginRequest request, HttpSession session) {
        Admin admin = adminRepository.findByUsernameIgnoreCase(request.username().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(AdminAuthenticationException::new);
        session.setAttribute(AdminSession.ADMIN_ID_ATTRIBUTE, admin.getId());
        return AdminResponse.from(admin);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    @Transactional(readOnly = true)
    public Admin requireAuthenticated(HttpSession session) {
        Object adminId = session.getAttribute(AdminSession.ADMIN_ID_ATTRIBUTE);
        if (!(adminId instanceof Long id)) {
            throw new UnauthorizedAdminException();
        }
        return adminRepository.findById(id).orElseThrow(UnauthorizedAdminException::new);
    }
}
