package ru.gesture.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import ru.gesture.model.User;
import ru.gesture.repository.UserRepository;
import ru.gesture.util.CookieUtil;

import java.util.Map;
import java.util.Optional;

@RestController @RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository users;
    private final CookieUtil     cookies;

    @PostMapping("/register")
    public Map<String, Boolean> register(@RequestBody Map<String, String> body,
                                         HttpServletResponse res) {
        String name = Optional.ofNullable(body.get("name")).orElse("").trim();
        if (name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty name");

        User user = users.findByName(name).orElseGet(() -> {
            User u = new User(); u.setName(name); return users.save(u);
        });

        cookies.setUid(res, user.getId());
        return Map.of("ok", true);
    }
}
