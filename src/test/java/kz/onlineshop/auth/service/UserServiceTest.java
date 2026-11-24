package kz.onlineshop.auth.service;

import kz.onlineshop.auth.model.LoginAttempt;
import kz.onlineshop.auth.model.Role;
import kz.onlineshop.auth.model.User;
import kz.onlineshop.auth.repository.LoginAttemptRepository;
import kz.onlineshop.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** UserService бизнес-логикасының unit-тесттері (дерекқорсыз, Mockito). */
class UserServiceTest {

    private static final String EMAIL = "aru@mail.kz";
    private static final String PASSWORD = "Almaty2026x";

    private UserRepository userRepository;
    private LoginAttemptRepository attemptRepository;
    private BCryptPasswordEncoder encoder;
    private MutableClock clock;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        attemptRepository = mock(LoginAttemptRepository.class);
        encoder = new BCryptPasswordEncoder(4); // тесттерде жылдам болу үшін аз раунд
        clock = new MutableClock(Instant.parse("2026-11-24T10:00:00Z"));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new UserService(userRepository, attemptRepository, encoder, clock, 5, 15);
    }

    private User existingUser() {
        User user = new User(EMAIL, encoder.encode(PASSWORD), Role.USER);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("Тіркелу: құпия сөз ашық түрде емес, BCrypt хэші түрінде сақталады")
    void registerStoresBcryptHash() {
        User user = service.register(EMAIL, PASSWORD, "127.0.0.1");

        assertThat(user.getPassword()).isNotEqualTo(PASSWORD).startsWith("$2a$");
        assertThat(encoder.matches(PASSWORD, user.getPassword())).isTrue();
        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("Бірдей құпия сөздің хэштері әртүрлі (кездейсоқ тұз)")
    void samePasswordGivesDifferentHashes() {
        String h1 = service.register("a1@mail.kz", PASSWORD, null).getPassword();
        String h2 = service.register("a2@mail.kz", PASSWORD, null).getPassword();

        assertThat(h1).isNotEqualTo(h2);
    }

    @Test
    @DisplayName("Тіркелу: email кіші әріпке келтіріліп, бос орындар алынады")
    void registerNormalizesEmail() {
        User user = service.register("  Aru@MAIL.kz ", PASSWORD, null);

        assertThat(user.getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Тіркелу: бос емес email қайта тіркелмейді")
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.register(EMAIL, PASSWORD, null))
            .isInstanceOf(RegistrationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Тіркелу: әлсіз құпия сөз қабылданбайды")
    void registerRejectsWeakPassword() {
        assertThatThrownBy(() -> service.register(EMAIL, "12345678", null))
            .isInstanceOf(RegistrationException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Кіру: дұрыс құпия сөз → true, қате санауыш нөлденеді")
    void loginSuccessResetsCounter() {
        User user = existingUser();
        user.setFailedAttempts(3);

        assertThat(service.login(EMAIL, PASSWORD, "127.0.0.1")).isTrue();
        assertThat(user.getFailedAttempts()).isZero();
    }

    @Test
    @DisplayName("Кіру: қате құпия сөз → false, санауыш артады")
    void loginWrongPasswordIncrementsCounter() {
        User user = existingUser();

        assertThat(service.login(EMAIL, "wrong-pass1", "127.0.0.1")).isFalse();
        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Brute-force: 5-ші қате әрекетте аккаунт 15 минутқа бұғатталады")
    void fifthFailureLocksAccount() {
        User user = existingUser();
        for (int i = 0; i < 4; i++) {
            assertThat(service.login(EMAIL, "wrong" + i, null)).isFalse();
        }

        assertThatThrownBy(() -> service.login(EMAIL, "wrong5", null))
            .isInstanceOf(AccountLockedException.class);
        assertThat(user.getLockedUntil()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Brute-force: бұғатталған аккаунтқа дұрыс құпия сөзбен де кіруге болмайды")
    void lockedAccountRejectsCorrectPassword() {
        User user = existingUser();
        user.setLockedUntil(clock.instant().plus(Duration.ofMinutes(10)));

        assertThatThrownBy(() -> service.login(EMAIL, PASSWORD, null))
            .isInstanceOf(AccountLockedException.class);
    }

    @Test
    @DisplayName("Brute-force: 15 минуттан кейін бұғат автоматты түрде алынады")
    void lockExpiresAfterDuration() {
        User user = existingUser();
        user.setLockedUntil(clock.instant().plus(Duration.ofMinutes(15)));

        clock.advance(Duration.ofMinutes(16));

        assertThat(service.login(EMAIL, PASSWORD, null)).isTrue();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Жоқ email: false қайтарады және журналға жазылады")
    void unknownEmailIsLogged() {
        when(userRepository.findByEmail("ghost@mail.kz")).thenReturn(Optional.empty());

        assertThat(service.login("ghost@mail.kz", "whatever1", "10.0.0.5")).isFalse();

        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(UserService.BAD_CREDENTIALS);
        assertThat(captor.getValue().getIpAddress()).isEqualTo("10.0.0.5");
        assertThat(captor.getValue().isSuccess()).isFalse();
    }

    @Test
    @DisplayName("Журнал: сәтті кіру SUCCESS ретінде жазылады")
    void successfulLoginIsLogged() {
        existingUser();

        service.login(EMAIL, PASSWORD, "127.0.0.1");

        ArgumentCaptor<LoginAttempt> captor = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(attemptRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo(UserService.SUCCESS);
        assertThat(captor.getValue().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Әкімші бұғатты қолмен алып тастай алады")
    void adminCanUnlockUser() {
        User user = existingUser();
        user.setFailedAttempts(2);
        user.setLockedUntil(clock.instant().plus(Duration.ofMinutes(5)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        service.unlockUser(7L);

        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getFailedAttempts()).isZero();
    }

    /** Тестте уақытты алға жылжытуға болатын сағат. */
    static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) { this.now = now; }

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
