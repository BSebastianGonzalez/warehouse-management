package co.wm.warehouse.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @InjectMocks
    private AdminService adminService;

    @Test
    void registersFirstAdminWithHashedPassword() {
        AdminRequest request = new AdminRequest("admin", "password123", "Main Admin");
        Admin saved = new Admin("admin", "hashed-password", "Main Admin");
        when(adminRepository.count()).thenReturn(0L);
        when(adminRepository.existsByUsernameIgnoreCase("admin")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(adminRepository.save(any(Admin.class))).thenReturn(saved);

        AdminResponse response = adminService.register(request);

        assertThat(response.username()).isEqualTo("admin");
        assertThat(response.name()).isEqualTo("Main Admin");
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void logsInAndStoresAdminIdInSession() {
        Admin admin = new Admin("admin", "hashed-password", "Main Admin");
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);

        AdminResponse response = adminService.login(
                new LoginRequest("admin", "password123"), session);

        assertThat(response.username()).isEqualTo("admin");
        verify(session).setAttribute(AdminSession.ADMIN_ID_ATTRIBUTE, admin.getId());
    }

    @Test
    void rejectsInvalidCredentials() {
        when(adminRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.login(
                new LoginRequest("admin", "wrong-password"), session))
                .isInstanceOf(AdminAuthenticationException.class);
    }

    @Test
    void listsAdministratorsWithoutExposingPasswords() {
        Admin admin = new Admin("admin", "hashed-password", "Main Admin");
        when(adminRepository.findAll()).thenReturn(List.of(admin));

        List<AdminResponse> response = adminService.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).username()).isEqualTo("admin");
        assertThat(response.get(0).name()).isEqualTo("Main Admin");
        verify(adminRepository).findAll();
    }
}
