package ru.gesture.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.gesture.model.Shot;
import ru.gesture.repository.ShotRepository;
import ru.gesture.util.CookieUtil;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnswerController {

    private final CookieUtil cookies;
    private final ShotRepository shots;

    public AnswerController(CookieUtil cookies, ShotRepository shots) {
        this.cookies = cookies;
        this.shots   = shots;
    }

    /** Обрабатывает ответ пользователя (ок/не ок) */
    @PostMapping("/answer")
    public Map<String, Boolean> answer(
            @RequestBody Map<String, Boolean> body,
            HttpServletRequest req
    ) {
        Boolean val = body.get("val");
        if (val == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "`val` missing in body");
        }

        long uid = cookies.readUid(req)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED));

        Shot shot = shots.findTopBySession_User_IdOrderByIdDesc(uid)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND));

        shot.setUserOk(val);
        shots.save(shot);

        return Map.of("ok", true);
    }
}