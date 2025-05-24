package ru.gesture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gesture.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
}
