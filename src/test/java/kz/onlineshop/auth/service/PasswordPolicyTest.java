package kz.onlineshop.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Құпия сөз саясатының тесттері. */
class PasswordPolicyTest {

    @Test
    @DisplayName("Мықты құпия сөз қабылданады")
    void strongPasswordAccepted() {
        assertThat(PasswordPolicy.validate("Almaty2026x", "aru@mail.kz")).isNull();
    }

    @ParameterizedTest(name = "әлсіз: \"{0}\"")
    @ValueSource(strings = {"short1", "onlyletters", "1234567890", "password1", "qwerty123"})
    @DisplayName("Әлсіз құпия сөздер қабылданбайды")
    void weakPasswordsRejected(String password) {
        assertThat(PasswordPolicy.validate(password, "x@mail.kz")).isNotNull();
    }

    @Test
    @DisplayName("Құпия сөзде email бөлігі болмауы керек")
    void passwordContainingEmailRejected() {
        assertThat(PasswordPolicy.validate("aigerim2026", "aigerim@mail.kz")).isNotNull();
    }

    @Test
    @DisplayName("72 байттан ұзын құпия сөз қабылданбайды (BCrypt шегі)")
    void tooLongPasswordRejected() {
        String longPassword = "a1".repeat(37); // 74 байт
        assertThat(PasswordPolicy.validate(longPassword, "x@mail.kz")).contains("72");
    }
}
