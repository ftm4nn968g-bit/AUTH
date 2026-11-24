package kz.onlineshop.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Онлайн дүкен үшін қауіпсіз тіркелу/кіру жүйесі.
 * Іске қосу: ./mvnw spring-boot:run  →  http://localhost:8080
 */
@SpringBootApplication
public class SecureAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureAuthApplication.class, args);
    }
}
