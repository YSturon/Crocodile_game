package ru.gesture.controller;

import com.fasterxml.jackson.annotation.JsonAlias;          // ★ new
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.gesture.dto.ResultMessage;
import ru.gesture.model.Session;
import ru.gesture.model.Shot;
import ru.gesture.model.User;
import ru.gesture.repository.SessionRepository;
import ru.gesture.repository.ShotRepository;
import ru.gesture.repository.UserRepository;
import ru.gesture.util.CookieUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LandmarkController {

    /* ---------- константы ---------- */
    private static final int    WINDOW     = 30;
    private static final int    FEAT_LEN   = 225;
    private static final int    FULL_LEN   = WINDOW * FEAT_LEN;
    private static final float  MIN_CONF   = 0.55f;
    private static final int    STABLE_CNT = 3;
    private static final long   ML_TIMEOUT = 2_000;     // ms
    private static final String SITE_VER   = "web-v10";

    /* ---------- DI ---------- */
    private final WebClient         ml;
    private final ShotRepository    shots;
    private final SessionRepository sessions;
    private final UserRepository    users;

    /* ---------- кольцевой буфер ---------- */
    private final float[][] buf = new float[WINDOW][FEAT_LEN];
    private int head = 0, filled = 0;

    /* =================================================================== */
    @MessageMapping("/landmarks")
    @SendToUser("/queue/result")
    public Mono<ResultMessage> handle(
            @Header("simpSessionId") String sid,
            @Payload @NotNull List<Float> seq,
            SimpMessageHeaderAccessor sha) {

        /* ---------- 1. собираем окно из 30 кадров ---------- */
        ByteBuffer bb;
        if (seq.size() == FEAT_LEN) {
            for (int i = 0; i < FEAT_LEN; i++) buf[head][i] = seq.get(i);
            head   = (head + 1) % WINDOW;
            filled = Math.min(filled + 1, WINDOW);
            if (filled < WINDOW) return Mono.empty();

            bb = ByteBuffer.allocate(FULL_LEN * 4).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < WINDOW; i++)
                for (float v : buf[(head + i) % WINDOW]) bb.putFloat(v);

        } else if (seq.size() == FULL_LEN) {
            bb = ByteBuffer.allocate(FULL_LEN * 4).order(ByteOrder.LITTLE_ENDIAN);
            seq.forEach(bb::putFloat);
        } else {
            log.warn("[{}] bad frame len {}", sid, seq.size());
            return Mono.empty();
        }

        /* ---------- 2. вызов ML ---------- */
        String b64 = Base64.getEncoder().encodeToString(bb.array());

        return ml.post()
                .uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("b64", b64))
                .retrieve()
                .bodyToMono(MlResp.class)
                .timeout(Duration.ofMillis(ML_TIMEOUT))
                .onErrorReturn(new MlResp("unknown", 0f, List.of()))
                /* ---------- 3. сохранение + ответ фронту ---------- */
                .flatMap(r -> Mono.fromCallable(() -> {
                    boolean saved = maybeSaveShot(sha, r);
                    return new ResultMessage(r.animal, r.confidence, r.probs, saved);
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    /* =================================================================== */
    private boolean maybeSaveShot(SimpMessageHeaderAccessor sha, MlResp r) {

        if (r.confidence < MIN_CONF) return false;

        /* --- UID (заголовок или cookie-атрибут) --- */
        Long uid = Optional.ofNullable(sha.getNativeHeader("uid"))
                .flatMap(h -> h.stream().findFirst())
                .map(Long::valueOf)
                .orElseGet(() -> {
                    Object a = sha.getSessionAttributes().get(CookieUtil.UID);
                    return a instanceof Long ? (Long) a : null;
                });
        if (uid == null) return false;

        /* --- три подряд уверенных предсказания --- */
        Map<String,Object> at = sha.getSessionAttributes();
        String last = (String) at.get("lastAnimal");
        int cnt     = (Integer) at.getOrDefault("cnt", 0);

        if (r.animal.equals(last)) cnt++; else { last = r.animal; cnt = 1; }
        at.put("lastAnimal", last);  at.put("cnt", cnt);

        if (cnt < STABLE_CNT) return false;
        at.put("cnt", 0);

        /* --- JPA --- */
        User user = users.findById(uid).orElse(null);
        if (user == null) return false;

        Session sess = sessions.findTopByUser_IdOrderByIdDesc(uid)
                .orElseGet(() -> sessions.save(new Session(user, SITE_VER)));

        shots.save(new Shot(sess, r.animal, r.confidence));
        return true;
    }

    /* =================================================================== */
    /** DTO ⇆ JSON от ML-сервиса */
    public static final class MlResp {
        public String animal;

        /** принимаем и «confidence», и «conf» */
        @JsonAlias({"confidence","conf"})
        public float confidence;

        public List<Float> probs;

        /* конструктор без аргументов нужен Jackson */
        public MlResp() { }

        public MlResp(String a, float c, List<Float> p){
            this.animal = a; this.confidence = c; this.probs = p;
        }
    }
}
