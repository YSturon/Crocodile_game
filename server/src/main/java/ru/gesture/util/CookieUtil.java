package ru.gesture.util;

import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class CookieUtil {

    public static final String UID = "zoo_uid";

    /** ставит cookie zoo_uid = <id> на год, SameSite=Lax */
    public void setUid(HttpServletResponse res, Long id) {
        ResponseCookie ck = ResponseCookie.from(UID, id.toString())
                .path("/")
                .maxAge(Duration.ofDays(365))
                .sameSite("Lax")    // для fetch / WebSocket с того же домена
                .httpOnly(false)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, ck.toString());
    }

    /** читает cookie zoo_uid, если она есть */
    public Optional<Long> readUid(HttpServletRequest req) {
        Cookie[] cookies = Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]);
        return Arrays.stream(cookies)
                .filter(c -> UID.equals(c.getName()))
                .findFirst()
                .map(c -> Long.valueOf(c.getValue()));
    }
}
