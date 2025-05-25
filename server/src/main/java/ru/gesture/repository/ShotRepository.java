package ru.gesture.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.gesture.model.Shot;
import ru.gesture.dto.StatRow;

import java.util.List;
import java.util.Optional;           // ← новый импорт

public interface ShotRepository extends JpaRepository<Shot, Long> {

    Optional<Shot> findTopBySession_UserIdOrderByIdDesc(long userId);

    List<Shot> findTop5BySession_IdOrderByIdDesc(long sessionId);

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
