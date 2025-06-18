package ru.gesture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.gesture.model.Shot;
import ru.gesture.dto.StatRow;

import java.util.List;
import java.util.Optional;

public interface ShotRepository extends JpaRepository<Shot, Long> {

    /**
     * Возвращает до 100 последних кадров пользователя,
     * используя вложенное свойство session.user.id
     */
    List<Shot> findTop100BySession_User_IdOrderByIdDesc(Long userId);

    /**
     * Возвращает последний кадр пользователя для AnswerController
     */
    Optional<Shot> findTopBySession_User_IdOrderByIdDesc(Long userId);

    /**
     * Последние 5 кадров конкретной сессии
     */
    List<Shot> findTop5BySession_IdOrderByIdDesc(Long sessionId);

    /**
     * Агрегированная сводка по всем пользователям
     */
    @Query("""
       select new ru.gesture.dto.StatRow(
               u.name,
               count(s),
               sum(case when s.userOk = true then 1 else 0 end)
       )
       from Shot s
       join s.session sess
       join sess.user u
       group by u.name
       """)
    List<StatRow> aggregate();
}