package co.wm.warehouse.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String name;

    protected Admin() {
    }

    public Admin(String username, String passwordHash, String name) {
        if (username == null || username.isBlank() || passwordHash == null || passwordHash.isBlank()
                || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Admin username, password hash and name are required");
        }
        this.username = username;
        this.passwordHash = passwordHash;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getName() { return name; }
}
