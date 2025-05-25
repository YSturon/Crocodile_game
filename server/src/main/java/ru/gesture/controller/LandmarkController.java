package ru.gesture.controller;

import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import ru.gesture.dto.ResultMessage;
import ru.gesture.model.*;
import ru.gesture.repository.*;
import ru.gesture.util.CookieUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class LandmarkController {

    private static final List<String> CLASSES = List.of("moose", "bull");
    private static final int WINDOW = 5;

    private final UserRepository    users;
    private final SessionRepository sessions;
    private final ShotRepository    shots;
    private final Map<String, Deque<String>> last5 = new HashMap<>();

    public LandmarkController(UserRepository users,
                              SessionRepository sessions,
                              ShotRepository shots) {
        this.users    = users;
        this.sessions = sessions;
        this.shots    = shots;
    }

    @MessageMapping("/landmarks")
    @SendToUser("/queue/result")
    public ResultMessage handle(@Header("simpSessionId") String sid,
                                @Payload String b64,
                                SimpMessageHeaderAccessor accessor) {

        var rnd    = ThreadLocalRandom.current();
        String animal = CLASSES.get(rnd.nextInt(CLASSES.size()));
        float  conf   = rnd.nextFloat();

        Deque<String> q = last5.computeIfAbsent(sid, k -> new ArrayDeque<>());
        q.add(animal);
        boolean finalShot = q.size() == WINDOW;

        if (finalShot) {
            q.clear();

            Long uid = (Long) accessor.getSessionAttributes().get(CookieUtil.UID);
            if (uid != null) {
                users.findById(uid).ifPresent(user -> {
                    Session sess = sessions
                            .findTopByUser_IdOrderByIdDesc(uid)
                            .orElseGet(() -> sessions.save(
                                    new Session(user, "v2-landmarks")));
                    shots.save(new Shot(sess, animal, conf));
                });
            }
        }
        return new ResultMessage(animal, conf, finalShot);
    }
}
