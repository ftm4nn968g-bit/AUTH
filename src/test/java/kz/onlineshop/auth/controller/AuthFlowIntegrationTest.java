package kz.onlineshop.auth.controller;

import kz.onlineshop.auth.model.User;
import kz.onlineshop.auth.repository.LoginAttemptRepository;
import kz.onlineshop.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Толық веб-тізбек: HTTP → Controller → Service → H2 дерекқоры. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.admin.password=Admin2026test",
    "security.bcrypt.strength=4"
})
class AuthFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired LoginAttemptRepository attemptRepository;

    @BeforeEach
    void clean() {
        attemptRepository.deleteAll();
        userRepository.findAll().stream()
            .filter(u -> !u.getEmail().equals("admin@shop.kz"))
            .forEach(userRepository::delete);
    }

    private void register(String email, String password) throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("email", email).param("password", password).param("confirmPassword", password))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));
    }

    @Test
    @DisplayName("Ашық беттер кіруді талап етпейді")
    void publicPagesAreOpen() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/register")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Тіркелу: дерекқорда тек BCrypt хэші сақталады")
    void registrationStoresHashInDatabase() throws Exception {
        register("dana@mail.kz", "Shymkent2026x");

        User saved = userRepository.findByEmail("dana@mail.kz").orElseThrow();
        assertThat(saved.getPassword()).startsWith("$2a$").doesNotContain("Shymkent2026x");
    }

    @Test
    @DisplayName("Тіркелу: құпия сөздер сәйкес келмесе, қате көрсетіледі")
    void registrationPasswordMismatch() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("email", "x@mail.kz").param("password", "Shymkent2026x").param("confirmPassword", "Other2026x"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "confirmPassword"));
        assertThat(userRepository.existsByEmail("x@mail.kz")).isFalse();
    }

    @Test
    @DisplayName("CSRF токенсіз POST сұраныс 403 қайтарады")
    void postWithoutCsrfIsForbidden() throws Exception {
        mvc.perform(post("/login").param("email", "a@mail.kz").param("password", "x"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Дұрыс кіру → кабинетке бағыттау, сессия жасалады")
    void successfulLoginOpensAccount() throws Exception {
        register("aru@mail.kz", "Almaty2026x");

        MvcResult result = mvc.perform(post("/login").with(csrf())
                .param("email", "aru@mail.kz").param("password", "Almaty2026x"))
            .andExpect(redirectedUrl("/account"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/account").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("aru@mail.kz")));
    }

    @Test
    @DisplayName("Қате құпия сөз → 401 және жалпы хабарлама")
    void wrongPasswordShowsGenericError() throws Exception {
        register("aru@mail.kz", "Almaty2026x");

        mvc.perform(post("/login").with(csrf())
                .param("email", "aru@mail.kz").param("password", "Wrong2026x"))
            .andExpect(status().isUnauthorized())
            .andExpect(model().attribute("error", AuthController.BAD_CREDENTIALS_MSG));
    }

    @Test
    @DisplayName("5 қате әрекеттен кейін дұрыс құпия сөз де қабылданбайды")
    void bruteForceLocksAccountEndToEnd() throws Exception {
        register("aru@mail.kz", "Almaty2026x");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/login").with(csrf())
                .param("email", "aru@mail.kz").param("password", "Wrong" + i + "abc"));
        }

        mvc.perform(post("/login").with(csrf())
                .param("email", "aru@mail.kz").param("password", "Almaty2026x"))
            .andExpect(status().isUnauthorized())
            .andExpect(model().attribute("locked", true));
        assertThat(attemptRepository.count()).isEqualTo(7); // 1 тіркелу + 5 қате + 1 бұғатталған
    }

    @Test
    @DisplayName("Кірмеген пайдаланушы кабинетке кіре алмайды → /login")
    void anonymousRedirectedToLogin() throws Exception {
        mvc.perform(get("/account")).andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(username = "aru@mail.kz", roles = "USER")
    @DisplayName("USER рөлі әкімші панеліне кіре алмайды → 403")
    void userCannotOpenAdmin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Әкімші кіреді және журналды көреді")
    void adminSeesLogs() throws Exception {
        MvcResult result = mvc.perform(post("/login").with(csrf())
                .param("email", "admin@shop.kz").param("password", "Admin2026test"))
            .andExpect(redirectedUrl("/admin"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/admin").session(session))
            .andExpect(status().isOk())
            .andExpect(model().attributeExists("users", "attempts"));
    }

    @Test
    @DisplayName("Қауіпсіздік HTTP тақырыптары қойылған")
    void securityHeadersPresent() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().exists("Content-Security-Policy"));
    }
}
