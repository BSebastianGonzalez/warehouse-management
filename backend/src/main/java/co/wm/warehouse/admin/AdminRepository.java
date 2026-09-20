package co.wm.warehouse.admin;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
