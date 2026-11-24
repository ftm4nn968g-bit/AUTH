package kz.onlineshop.auth.repository;

import kz.onlineshop.auth.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Кіру журналымен жұмыс. */
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    List<LoginAttempt> findTop100ByOrderByCreatedAtDesc();

    List<LoginAttempt> findTop10ByEmailOrderByCreatedAtDesc(String email);

    long countBySuccessFalse();
}
