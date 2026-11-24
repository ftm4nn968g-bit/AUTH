package kz.onlineshop.auth.service;

import kz.onlineshop.auth.model.LoginAttempt;
import kz.onlineshop.auth.model.Role;
import kz.onlineshop.auth.model.User;
import kz.onlineshop.auth.repository.LoginAttemptRepository;
import kz.onlineshop.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Бизнес-логика: тіркелу, кіру, brute-force қорғау, журнал.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public static final String SUCCESS = "SUCCESS";
    public static final String BAD_CREDENTIALS = "BAD_CREDENTIALS";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String LOCKED_NOW = "LOCKED_NOW";
    public static final String REGISTERED = "REGISTERED";

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Clock clock;
    private final int maxAttempts;
    private final Duration lockDuration;

    /**
     * Жоқ email үшін де BCrypt тексеруі орындалады — жауап уақыты бірдей болып,
     * шабуылдаушы email-дің бар-жоғын уақыт бойынша біле алмайды (timing attack).
     */
    private final String dummyHash;

    public UserService(UserRepository userRepository,
                       LoginAttemptRepository loginAttemptRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       Clock clock,
                       @Value("${security.login.max-attempts:5}") int maxAttempts,
                       @Value("${security.login.lock-minutes:15}") int lockMinutes) {
        this.userRepository = userRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /** Жаңа сатып алушыны тіркейді. Құпия сөз BCrypt хэші түрінде ғана сақталады. */
    @Transactional
    public User register(String email, String rawPassword, String ip) {
        String normalized = normalizeEmail(email);
        String policyError = PasswordPolicy.validate(rawPassword, normalized);
        if (policyError != null) {
            throw new RegistrationException(policyError);
        }
        if (userRepository.existsByEmail(normalized)) {
            throw new RegistrationException("Бұл email бойынша аккаунт бұрыннан бар");
        }
        User user = userRepository.save(new User(normalized, passwordEncoder.encode(rawPassword), Role.USER));
        audit(normalized, ip, true, REGISTERED);
        log.info("Жаңа пайдаланушы тіркелді: {}", normalized);
        return user;
    }

    /**
     * Кіруді тексереді.
     *
     * @return true — email мен құпия сөз дұрыс; false — қате
     * @throws AccountLockedException аккаунт бұғатталған (немесе осы әрекетте бұғатталды)
     */
    @Transactional(noRollbackFor = AccountLockedException.class)
    public boolean login(String email, String rawPassword, String ip) {
        String normalized = normalizeEmail(email);
        Optional<User> found = userRepository.findByEmail(normalized);

        if (found.isEmpty()) {
            passwordEncoder.matches(rawPassword == null ? "" : rawPassword, dummyHash);
            audit(normalized, ip, false, BAD_CREDENTIALS);
            return false;
        }

        User user = found.get();
        try {
            checkBruteForce(user);
        } catch (AccountLockedException e) {
            audit(normalized, ip, false, ACCOUNT_LOCKED);
            throw e;
        }

        if (verifyPassword(rawPassword, user.getPassword())) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            audit(normalized, ip, true, SUCCESS);
            return true;
        }

        int failed = user.getFailedAttempts() + 1;
        if (failed >= maxAttempts) {
            Instant until = Instant.now(clock).plus(lockDuration);
            user.setFailedAttempts(0);
            user.setLockedUntil(until);
            userRepository.save(user);
            audit(normalized, ip, false, LOCKED_NOW);
            log.warn("Brute-force күдігі: {} аккаунты {} дейін бұғатталды (IP {})", normalized, until, ip);
            throw new AccountLockedException(until);
        }
        user.setFailedAttempts(failed);
        userRepository.save(user);
        audit(normalized, ip, false, BAD_CREDENTIALS);
        return false;
    }

    /** Аккаунт бұғатталған болса, {@link AccountLockedException} лақтырады. */
    public void checkBruteForce(User user) {
        if (user.isLocked(Instant.now(clock))) {
            throw new AccountLockedException(user.getLockedUntil());
        }
    }

    /** BCrypt арқылы құпия сөзді хэшпен салыстырады (тұз хэштің ішінде сақталған). */
    public boolean verifyPassword(String rawPassword, String hash) {
        return rawPassword != null && passwordEncoder.matches(rawPassword, hash);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<LoginAttempt> getRecentAttempts() {
        return loginAttemptRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public List<LoginAttempt> getRecentAttempts(String email) {
        return loginAttemptRepository.findTop10ByEmailOrderByCreatedAtDesc(normalizeEmail(email));
    }

    public long countFailedAttempts() {
        return loginAttemptRepository.countBySuccessFalse();
    }

    /** Әкімші бұғатты қолмен алып тастайды. */
    @Transactional
    public void unlockUser(Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            log.info("Әкімші бұғатты алды: {}", user.getEmail());
        });
    }

    /** Әкімші пайдаланушыны жояды (өзін жоя алмайды). */
    @Transactional
    public void deleteUser(Long id, String currentAdminEmail) {
        userRepository.findById(id).ifPresent(user -> {
            if (user.getEmail().equals(normalizeEmail(currentAdminEmail))) {
                throw new IllegalArgumentException("Әкімші өз аккаунтын жоя алмайды");
            }
            userRepository.delete(user);
            log.info("Әкімші пайдаланушыны жойды: {}", user.getEmail());
        });
    }

    private void audit(String email, String ip, boolean success, String result) {
        loginAttemptRepository.save(new LoginAttempt(email, ip, success, result, Instant.now(clock)));
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
