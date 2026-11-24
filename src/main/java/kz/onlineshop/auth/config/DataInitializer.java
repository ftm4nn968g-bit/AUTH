package kz.onlineshop.auth.config;

import kz.onlineshop.auth.model.Role;
import kz.onlineshop.auth.model.User;
import kz.onlineshop.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

/**
 * Бірінші іске қосылғанда әкімші аккаунтын жасайды.
 * Құпия сөз кодта жазылмайды: ADMIN_PASSWORD орта айнымалысынан алынады,
 * ол болмаса — JCA {@link SecureRandom} арқылы кездейсоқ генерацияланады.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           @Value("${app.admin.email}") String adminEmail,
                           @Value("${app.admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        String password = adminPassword;
        if (password == null || password.isBlank()) {
            byte[] bytes = new byte[12];
            new SecureRandom().nextBytes(bytes);
            password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "7a";
            log.warn("=================================================================");
            log.warn(" ADMIN_PASSWORD орнатылмаған. Уақытша әкімші құпия сөзі:");
            log.warn("   email:    {}", adminEmail);
            log.warn("   password: {}", password);
            log.warn("=================================================================");
        }
        userRepository.save(new User(adminEmail, passwordEncoder.encode(password), Role.ADMIN));
        log.info("Әкімші аккаунты жасалды: {}", adminEmail);
    }
}
