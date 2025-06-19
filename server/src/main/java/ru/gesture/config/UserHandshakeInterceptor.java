package ru.gesture.config;

import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ru.gesture.util.CookieUtil;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Перехватывает WS-рукопожатие, извлекает UID (cookie / query / header)
 * и кладёт его в атрибуты сессии под ключом CookieUtil.UID.
 */
@Slf4j
public class UserHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        Long uid = null;

        /* ───────────────── 1. cookie zoo_uid ───────────────── */
        if (request instanceof ServletServerHttpRequest srvReq) {
            HttpServletRequest req = srvReq.getServletRequest();
            uid = Arrays.stream(
                            Optional.ofNullable(req.getCookies())
                                    .orElse(new Cookie[0]))
                    .filter(c -> CookieUtil.UID.equals(c.getName()))
                    .findFirst()
                    .map(c -> Long.valueOf(c.getValue()))
                    .orElse(null);
        }

        /* ───────────────── 2. query-параметр /ws?uid=123 ───────────────── */
        if (uid == null) {
            String q = request.getURI().getRawQuery();          // без декодирования
            if (q != null) {
                for (String pair : q.split("&")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2 && "uid".equals(kv[0])) {
                        try {
                            uid = Long.valueOf(
                                    URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                        } catch (NumberFormatException ignore) { }
                        break;
                    }
                }
            }
        }

        /* ───────────────── 3. заголовок CONNECT uid:123 ───────────────── */
        if (uid == null) {
            List<String> h = request.getHeaders().get("uid");
            if (h != null && !h.isEmpty()) {
                try { uid = Long.valueOf(h.get(0)); }           // ← get(0) вместо getFirst()
                catch (NumberFormatException ignore) { }
            }
        }

        /* ───────────────── кладём в атрибуты, если нашли ───────────────── */
        if (uid != null) {
            attributes.put(CookieUtil.UID, uid);
            log.debug("WS handshake: {}={}", CookieUtil.UID, uid);
        } else {
            log.warn("WS handshake: UID NOT FOUND (cookie/query/header)");
        }

        return true;    // всегда разрешаем handshake
    }

    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                               WebSocketHandler wsHandler, Exception ex) { }
}
