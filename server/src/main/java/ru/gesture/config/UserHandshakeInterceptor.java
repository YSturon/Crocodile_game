package ru.gesture.config;

import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ru.gesture.util.CookieUtil;

import java.util.*;

/** Извлекает cookie zoo_uid и кладёт её в атрибуты WebSocket-сессии. */
@Slf4j
public class UserHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest srvReq) {
            HttpServletRequest req = srvReq.getServletRequest();
            Optional<Long> uid = Arrays.stream(
                            Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]))
                    .filter(c -> CookieUtil.UID.equals(c.getName()))
                    .findFirst()
                    .map(c -> Long.valueOf(c.getValue()));

            uid.ifPresent(id -> {
                attributes.put(CookieUtil.UID, id);
                log.debug("WS handshake: {}={}", CookieUtil.UID, id);
            });

            if (uid.isEmpty())
                log.warn("WS handshake: cookie {} NOT FOUND", CookieUtil.UID);
        }
        return true;   // продолжаем всегда
    }

    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                               WebSocketHandler wsHandler, Exception ex) { }
}
