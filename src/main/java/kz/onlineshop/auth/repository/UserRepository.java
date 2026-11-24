package kz.onlineshop.auth.repository;

import kz.onlineshop.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Пайдаланушылар кестесімен жұмыс. SQL-инъекциядан JPA параметрлері қорғайды. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
