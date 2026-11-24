package kz.onlineshop.auth.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Пайдаланушы. Құпия сөз ашық түрде ЕШҚАШАН сақталмайды —
 * {@code password} өрісінде тек BCrypt хэші болады ($2a$12$...).
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    /** BCrypt хэші (60 таңба). */
    @Column(nullable = false, length = 60)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role = Role.USER;

    /** Қатарынан қате енгізілген құпия сөз саны (brute-force қорғау). */
    @Column(nullable = false)
    private int failedAttempts = 0;

    /** Осы уақытқа дейін аккаунт бұғатталған; null болса — бұғатталмаған. */
    private Instant lockedUntil;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {
        // JPA үшін
    }

    public User(String email, String passwordHash, Role role) {
        this.email = email;
        this.password = passwordHash;
        this.role = role;
    }

    /** Аккаунт берілген уақытта бұғатталған ба? */
    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Role getRole() { return role; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getCreatedAt() { return createdAt; }

    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public void setLockedUntil(Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public void setRole(Role role) { this.role = role; }
}
