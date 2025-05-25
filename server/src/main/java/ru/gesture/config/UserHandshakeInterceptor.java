package ru.gesture.config;

import jakarta.servlet.http.*;
import org.springframework.http.server.*;
import org.springframework.web.socket.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import ru.gesture.util.CookieUtil;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;


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

            uid.ifPresent(id -> attributes.put(CookieUtil.UID, id));
        }
        return true;
    }

    @Override public void afterHandshake(ServerHttpRequest req,
                                         ServerHttpResponse res,
                                         WebSocketHandler wsHandler,
                                         Exception ex) {  }
}
