package ru.gesture.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.gesture.model.User;
import ru.gesture.repository.UserRepository;
import ru.gesture.util.CookieUtil;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository users;
    private final CookieUtil     cookies;

    @Autowired
    public UserController(UserRepository users, CookieUtil cookies) {
        this.users   = users;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    public Map<String, Boolean> register(@RequestBody Map<String, String> body,
                                         HttpServletResponse res) {

        String name = Optional.ofNullable(body.get("name")).orElse("").trim();
        if (name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "empty name");

        User user = users.findByName(name)
                .orElseGet(() -> users.save(new User(name)));

        cookies.setUid(res, user.getId());
        return Map.of("ok", true);
    }
}
