package ru.gesture.repository;

import org.springframework.data.jpa.repository.*;
import ru.gesture.model.Shot;
import ru.gesture.dto.StatRow;

import java.util.List;
import java.util.Optional;

public interface ShotRepository extends JpaRepository<Shot, Long> {

    @Query("""
         SELECT new ru.gesture.dto.StatRow(s.session.user.name,
                                           COUNT(s.id),
                                           SUM(CASE WHEN s.userOk = TRUE THEN 1 ELSE 0 END))
         FROM Shot s
         GROUP BY s.session.user.name
         """)
    List<StatRow> aggregate();

    Optional<Shot> findTopBySession_User_IdOrderByIdDesc(Long userId);
}
