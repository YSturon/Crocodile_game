package ru.gesture.controller;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Обрабатывает последовательности Landmark-ов, зовёт /predict и,
 * при достаточной уверенности, пишет кадр в БД.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class LandmarkController {

    /* ---------- константы ---------- */
    private static final int   WINDOW     = 30;
    private static final int   FEAT_LEN   = 225;
    private static final int   FULL_LEN   = WINDOW * FEAT_LEN;
    private static final float MIN_CONF   = 0.55f;
    private static final long  ML_TIMEOUT = 2_000;              // мс
    private static final String SITE_VER  = "web-v10";

    /* ---------- DI ---------- */
    private final WebClient         ml;
    private final ShotRepository    shots;
    private final SessionRepository sessions;
    private final UserRepository    users;

    /* ---------- скользящее окно ---------- */
    private final float[][] buf = new float[WINDOW][FEAT_LEN];
    private int head = 0, filled = 0;

    /* =================================================================== */
    @MessageMapping("/landmarks")
    @SendToUser("/queue/result")
    @Transactional
    public Mono<ResultMessage> handle(@Header("simpSessionId") String sid,
                                      @Payload @NotNull List<Float> seq,
                                      SimpMessageHeaderAccessor sha) {

        /* -------- 1. собираем окно 30×225 или берём готовые 6750 float -------- */
        ByteBuffer bb;
        if (seq.size() == FEAT_LEN) {                 // пришёл один кадр
            for (int i = 0; i < FEAT_LEN; i++) buf[head][i] = seq.get(i);
            head   = (head + 1) % WINDOW;
            filled = Math.min(filled + 1, WINDOW);
            if (filled < WINDOW) return Mono.empty(); // ждём 30 кадров

            bb = ByteBuffer.allocate(FULL_LEN * 4).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < WINDOW; i++)
                for (float v : buf[(head + i) % WINDOW]) bb.putFloat(v);
        } else if (seq.size() == FULL_LEN) {          // сразу блок 30 кадров
            bb = ByteBuffer.allocate(FULL_LEN * 4).order(ByteOrder.LITTLE_ENDIAN);
            seq.forEach(bb::putFloat);
        } else {
            log.warn("[{}] Bad frame length: {}", sid, seq.size());
            return Mono.empty();
        }

        /* -------- 2. FastAPI /predict -------- */
        String b64 = Base64.getEncoder().encodeToString(bb.array());

        return ml.post().uri("/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("b64", b64))
                .retrieve()
                .bodyToMono(MlResp.class)
                .timeout(Duration.ofMillis(ML_TIMEOUT))
                .onErrorResume(ex -> Mono.just(new MlResp("unknown", 0f, List.of())))
                .map(r -> new ResultMessage(
                        r.animal, r.confidence, r.probs,
                        saveIfConfident(sha, r)));
    }

    /* ---------- сохраняем Shot, если уверенность ≥ MIN_CONF ---------- */
    private boolean saveIfConfident(SimpMessageHeaderAccessor sha, MlResp r) {

        if (r.confidence < MIN_CONF) return false;

        /* 1. Пытаемся взять UID из атрибутов WebSocket-сессии */
        Long tmp = Optional.ofNullable(sha.getSessionAttributes())
                .map(m -> m.get(CookieUtil.UID))
                .filter(Long.class::isInstance)
                .map(Long.class::cast)
                .orElse(null);

        /* 2. Если нет — берём из заголовка STOMP (см. ws.js) */
        if (tmp == null) {
            String h = sha.getFirstNativeHeader("uid");
            if (h != null && !h.isBlank())
                try { tmp = Long.parseLong(h); } catch (NumberFormatException ignore) { }
        }
        if (tmp == null) return false;

        final Long uid = tmp;                         // effectively-final

        User user = users.findById(uid).orElse(null);
        if (user == null) return false;

        Session sess = sessions.findTopByUser_IdOrderByIdDesc(uid)
                .orElseGet(() -> sessions.save(new Session(user, SITE_VER)));

        shots.save(new Shot(sess, r.animal, r.confidence));
        return true;
    }

    /* ---------- DTO FastAPI ---------- */
    public record MlResp(String animal, float confidence, List<Float> probs) {}
}
