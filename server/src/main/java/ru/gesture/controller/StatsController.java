package ru.gesture.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.gesture.model.Shot;
import ru.gesture.repository.ShotRepository;
import ru.gesture.util.CookieUtil;

import java.util.List;

@RestController
public class StatsController {

    private final ShotRepository shots;
    private final CookieUtil     cookies;

    public StatsController(ShotRepository shots, CookieUtil cookies) {
        this.shots   = shots;
        this.cookies = cookies;
    }

    /** история последнего пользователя — до 100 записей */
    @GetMapping("/api/stats")
    public List<Row> stats(HttpServletRequest req) {
        return cookies.readUid(req)
                .map(uid -> shots.findTop100BySession_User_IdOrderByIdDesc(uid)
                        .stream().map(Row::new).toList())
                .orElse(List.of());
    }

    /** компактный DTO для фронта */
    record Row(Long id, String utc, String user, String animal, float conf) {
        Row(Shot s) {
            this(s.getId(),
                    s.getCreatedAt().toString(),
                    s.getSession().getUser().getName(),
                    s.getAnimal(),
                    s.getConf());
        }
    }
}
