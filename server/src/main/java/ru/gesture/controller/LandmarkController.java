package ru.gesture.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.gesture.dto.ResultMessage;
import ru.gesture.model.Session;
import ru.gesture.model.Shot;
import ru.gesture.repository.SessionRepository;
import ru.gesture.repository.ShotRepository;
import ru.gesture.repository.UserRepository;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class LandmarkController {

    private static final Logger log        = LoggerFactory.getLogger(LandmarkController.class);
    private static final float  OK_CONF    = 0.50f;   // «уверенный» кадр
    private static final int    N_OK       = 5;       // подряд одинаковых
    private static final int    ML_TIMEOUT = 1_500;   // мс

    private final WebClient         ml;
    private final UserRepository    users;
    private final SessionRepository sessions;
    private final ShotRepository    shots;

    /** последний animal и длина текущей серии по STOMP-сессии */
    private final Map<String,String>  lastAnimal = new ConcurrentHashMap<>();
    private final Map<String,Integer> streak     = new ConcurrentHashMap<>();

    public LandmarkController(WebClient ml,
                              UserRepository users,
                              SessionRepository sessions,
                              ShotRepository shots) {
        this.ml       = ml;
        this.users    = users;
        this.sessions = sessions;
        this.shots    = shots;
    }

    /* ─────────── STOMP endpoint ─────────── */
    @MessageMapping("/landmarks")
    @SendToUser("/queue/result")
    public Mono<ResultMessage> handle(@Header("simpSessionId") String sid,
                                      @Payload String b64,
                                      @Header("simpSessionAttributes") Map<String,Object> attrs) {

        // пересылаем ML-сервису ту же base-64-строку, без преобразований
        return ml.post()
                .uri("/predict")
                .bodyValue(Map.of("b64", b64))
                .retrieve()
                .bodyToMono(MlResp.class)
                .timeout(Duration.ofMillis(ML_TIMEOUT))
                .onErrorResume(ex -> {
                    log.error("Error calling ML-service", ex);
                    return Mono.just(new MlResp("unknown", 0f, List.of()));
                })
                .map(r -> afterMl(r, sid, attrs));
    }

    /* ---------- расчёт серии и сохранение ---------- */
    private ResultMessage afterMl(MlResp r,
                                  String sid,
                                  Map<String,Object> attrs) {

        String prev = lastAnimal.get(sid);
        if (r.conf() > OK_CONF && r.animal().equals(prev))
            streak.merge(sid, 1, Integer::sum);
        else
            streak.put(sid, 1);
        lastAnimal.put(sid, r.animal());

        boolean finalShot = false;
        if (streak.get(sid) >= N_OK && r.conf() > OK_CONF) {
            finalShot = true;
            streak.put(sid, 0);
            saveShot(attrs, r.animal(), r.conf());
        }

        return new ResultMessage(r.animal(), r.conf(), r.probs(), finalShot);
    }

    /* ---------- helper ---------- */
    private void saveShot(Map<String,Object> attrs, String animal, float conf){
        Long uid = (Long) attrs.get("zoo_uid");
        if (uid == null) return;

        users.findById(uid).ifPresent(u -> {
            Session s = sessions.findTopByUser_IdOrderByIdDesc(uid)
                    .orElseGet(() -> sessions.save(new Session(u, "v3")));
            shots.save(new Shot(s, animal, conf));
        });
    }

    /* ответ FastAPI-сервиса */
    private record MlResp(String animal, float conf, List<Float> probs) { }
}