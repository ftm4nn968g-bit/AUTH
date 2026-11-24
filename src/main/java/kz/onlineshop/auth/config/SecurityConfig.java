package kz.onlineshop.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.security.SecureRandom;
import java.time.Clock;

/**
 * Қауіпсіздік баптаулары.
 * <ul>
 *   <li>BCrypt — құпия сөздерді тұзбен (salt) хэштейді, тұз JCA {@link SecureRandom} арқылы алынады;</li>
 *   <li>CSRF қорғауы қосулы (Spring Security әдепкі);</li>
 *   <li>/admin/** — тек ADMIN рөліне;</li>
 *   <li>қауіпсіздік HTTP тақырыптары: CSP, X-Frame-Options, Referrer-Policy.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.bcrypt.strength:12}")
    private int bcryptStrength;

    /** BCrypt шифрлаушысы. strength=12 → 2^12 = 4096 раунд. */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(bcryptStrength, new SecureRandom());
    }

    /** Сессияда SecurityContext сақтайтын репозиторий (AuthController қолданады). */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** Уақыт көзі — тесттерде бұғаттау уақытын тексеру үшін ауыстыруға болады. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/register", "/login", "/css/**", "/img/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            // Кіру формасын AuthController өзі өңдейді (UML-дегі тізбек бойынша)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .securityContext(ctx -> ctx.securityContextRepository(securityContextRepository()))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; "
                        + "frame-ancestors 'none'; form-action 'self'; base-uri 'self'"))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)));
        return http.build();
    }
}
