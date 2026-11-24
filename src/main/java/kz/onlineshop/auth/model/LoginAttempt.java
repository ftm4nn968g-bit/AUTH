package kz.onlineshop.auth.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Кіру әрекетінің журналдағы жазбасы (audit log).
 * Құпия сөздің өзі журналға ЕШҚАШАН жазылмайды.
 */
@Entity
@Table(name = "login_attempts", indexes = @Index(columnList = "createdAt"))
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private boolean success;

    /** SUCCESS, BAD_CREDENTIALS, ACCOUNT_LOCKED, LOCKED_NOW, REGISTERED */
    @Column(nullable = false, length = 30)
    private String result;

    @Column(nullable = false)
    private Instant createdAt;

    protected LoginAttempt() {
        // JPA үшін
    }

    public LoginAttempt(String email, String ipAddress, boolean success, String result, Instant createdAt) {
        this.email = email;
        this.ipAddress = ipAddress;
        this.success = success;
        this.result = result;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getIpAddress() { return ipAddress; }
    public boolean isSuccess() { return success; }
    public String getResult() { return result; }
    public Instant getCreatedAt() { return createdAt; }
}
