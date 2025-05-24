package ru.gesture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.gesture.model.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {}
