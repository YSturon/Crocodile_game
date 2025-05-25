package ru.gesture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gesture.model.Session;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findTopByUser_IdOrderByIdDesc(long userId);
}
