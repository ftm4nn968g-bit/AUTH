package kz.onlineshop.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Құпия сөз саясаты.
 * BCrypt тек алғашқы 72 байтты қолданады, сондықтан ұзындық 72 байттан аспауы керек.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    /** Ең жиі кездесетін әлсіз құпия сөздер (қысқа тізім). */
    private static final Set<String> COMMON = Set.of(
        "password", "password1", "12345678", "123456789", "1234567890", "qwerty123",
        "qwertyui", "11111111", "iloveyou", "admin123", "abc12345", "parol123", "qazaqstan1");

    private PasswordPolicy() {
    }

    /**
     * Құпия сөзді тексереді.
     * @return қате мәтіні немесе құпия сөз жарамды болса null
     */
    public static String validate(String password, String email) {
        if (password == null || password.length() < MIN_LENGTH) {
            return "Құпия сөз кемінде " + MIN_LENGTH + " таңбадан тұруы керек";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return "Құпия сөз тым ұзын (BCrypt шегі — 72 байт)";
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            return "Құпия сөзде әріп те, сан да болуы керек";
        }
        String lower = password.toLowerCase(Locale.ROOT);
        if (COMMON.contains(lower)) {
            return "Бұл құпия сөз тым жиі кездеседі, басқасын таңдаңыз";
        }
        if (email != null && !email.isBlank()) {
            String local = email.split("@")[0].toLowerCase(Locale.ROOT);
            if (local.length() >= 3 && lower.contains(local)) {
                return "Құпия сөзде email-дің бөлігі болмауы керек";
            }
        }
        return null;
    }
}
