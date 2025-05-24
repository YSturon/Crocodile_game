package ru.gesture.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.gesture.repository.ShotRepository;
import ru.gesture.util.CookieUtil;

import java.util.Map;

@RestController @RequestMapping("/api")
@RequiredArgsConstructor
public class AnswerController {

    private final CookieUtil    cookies;
    private final ShotRepository shots;

    @PostMapping("/answer")
    public Map<String, Boolean> answer(@RequestBody Map<String, Boolean> body,
                                       HttpServletRequest req) {

        boolean val = Boolean.TRUE.equals(body.get("val"));
        long uid = cookies.readUid(req)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        var shot = shots.findTopBySession_User_IdOrderByIdDesc(uid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        shot.setUserOk(val);
        shots.save(shot);
        return Map.of("ok", true);
    }
}
