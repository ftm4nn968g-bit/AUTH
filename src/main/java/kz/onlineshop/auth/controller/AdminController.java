package kz.onlineshop.auth.controller;

import kz.onlineshop.auth.model.User;
import kz.onlineshop.auth.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Әкімші панелі: журналдарды көру және пайдаланушыларды басқару.
 * Қолжетімділік SecurityConfig-те шектелген: /admin/** → тек ROLE_ADMIN.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<User> users = userService.getAllUsers().stream()
            .sorted(Comparator.comparing(User::getCreatedAt).reversed())
            .toList();
        Instant now = Instant.now();
        model.addAttribute("users", users);
        model.addAttribute("attempts", userService.getRecentAttempts());
        model.addAttribute("now", now);
        model.addAttribute("totalUsers", users.size());
        model.addAttribute("lockedUsers", users.stream().filter(u -> u.isLocked(now)).count());
        model.addAttribute("failedAttempts", userService.countFailedAttempts());
        return "admin";
    }

    @PostMapping("/users/{id}/unlock")
    public String unlock(@PathVariable Long id, RedirectAttributes redirect) {
        userService.unlockUser(id);
        redirect.addFlashAttribute("message", "Аккаунт бұғаттан шығарылды");
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        try {
            userService.deleteUser(id, authentication.getName());
            redirect.addFlashAttribute("message", "Пайдаланушы жойылды");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("message", e.getMessage());
        }
        return "redirect:/admin";
    }
}
