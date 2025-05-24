package ru.gesture.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.gesture.model.Shot;
import ru.gesture.repository.ShotRepository;
import ru.gesture.util.CookieUtil;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnswerController {

    private final CookieUtil     cookies;
    private final ShotRepository shots;

    @Autowired
    public AnswerController(CookieUtil cookies, ShotRepository shots) {
        this.cookies = cookies;
        this.shots   = shots;
    }

    @PostMapping("/answer")
    public Map<String, Boolean> answer(@RequestBody Map<String, Boolean> body,
                                       HttpServletRequest req) {

        boolean val = Boolean.TRUE.equals(body.get("val"));

        long uid = cookies.readUid(req)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Shot shot = shots.findTopBySession_User_IdOrderByIdDesc(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        shot.setUserOk(val);
        shots.save(shot);

        return Map.of("ok", true);
    }
}
