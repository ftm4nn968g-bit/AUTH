package kz.onlineshop.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kz.onlineshop.auth.dto.LoginForm;
import kz.onlineshop.auth.dto.RegisterForm;
import kz.onlineshop.auth.model.LoginAttempt;
import kz.onlineshop.auth.model.Role;
import kz.onlineshop.auth.model.User;
import kz.onlineshop.auth.service.AccountLockedException;
import kz.onlineshop.auth.service.RegistrationException;
import kz.onlineshop.auth.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Сыртқы сұраныстар: тіркелу, кіру, жеке кабинет.
 */
@Controller
public class AuthController {

    /** Қате кезінде бірдей хабарлама — email бар ма, жоқ па, білдірмейміз. */
    static final String BAD_CREDENTIALS_MSG = "Email немесе құпия сөз қате";

    private final UserService userService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserService userService, SecurityContextRepository securityContextRepository) {
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult binding,
                           HttpServletRequest request,
                           Model model) {
        if (!binding.hasErrors() && !form.getPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "Құпия сөздер сәйкес келмейді");
        }
        if (binding.hasErrors()) {
            form.setPassword(null);
            form.setConfirmPassword(null);
            return "register";
        }
        try {
            userService.register(form.getEmail(), form.getPassword(), request.getRemoteAddr());
        } catch (RegistrationException e) {
            model.addAttribute("error", e.getMessage());
            form.setPassword(null);
            form.setConfirmPassword(null);
            return "register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("form", new LoginForm());
        return "login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("form") LoginForm form,
                        BindingResult binding,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Model model) {
        if (binding.hasErrors()) {
            form.setPassword(null);
            return "login";
        }
        boolean ok;
        try {
            ok = userService.login(form.getEmail(), form.getPassword(), request.getRemoteAddr());
        } catch (AccountLockedException e) {
            long minutes = Math.max(1, Duration.between(Instant.now(), e.getLockedUntil()).toMinutes() + 1);
            model.addAttribute("error",
                "Тым көп сәтсіз әрекет. Аккаунт уақытша бұғатталды, " + minutes + " минуттан кейін қайталаңыз.");
            model.addAttribute("locked", true);
            form.setPassword(null);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "login";
        }
        if (!ok) {
            model.addAttribute("error", BAD_CREDENTIALS_MSG);
            form.setPassword(null);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return "login";
        }

        User user = userService.findByEmail(form.getEmail()).orElseThrow();
        signIn(user, request, response);
        return user.getRole() == Role.ADMIN ? "redirect:/admin" : "redirect:/account";
    }

    @GetMapping("/account")
    public String account(Authentication authentication, Model model) {
        User user = userService.findByEmail(authentication.getName()).orElseThrow();
        List<LoginAttempt> attempts =userService.getRecentAttempts(user.getEmail());
        model.addAttribute("user", user);
        model.addAttribute("attempts", attempts);
        return "account";
    }

    /**
     * Пайдаланушыны сессияға кіргізеді.
     * Session fixation шабуылынан қорғау үшін сессия ID-і жаңартылады.
     */
    private void signIn(User user, HttpServletRequest request, HttpServletResponse response) {
        request.getSession(true);
        request.changeSessionId();
        Authentication auth = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
