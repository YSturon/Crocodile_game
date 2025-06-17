package ru.gesture.util;

import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class CookieUtil {

    public static final String UID = "zoo_uid";

    public void setUid(HttpServletResponse res, Long id) {
        ResponseCookie ck = ResponseCookie.from(UID, id.toString())
                .path("/")
                .maxAge(Duration.ofDays(365))
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, ck.toString());
    }


    public Optional<Long> readUid(HttpServletRequest req) {
        Cookie[] cookies = Optional.ofNullable(req.getCookies()).orElse(new Cookie[0]);
        return Arrays.stream(cookies)
                .filter(c -> UID.equals(c.getName()))
                .findFirst()
                .map(c -> Long.valueOf(c.getValue()));
    }
}
