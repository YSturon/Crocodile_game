package ru.gesture.controller;

import org.springframework.web.bind.annotation.*;
import ru.gesture.dto.ResultMessage;

import java.util.*;

@RestController
@RequestMapping("/api")
public class LandmarkRestController {

    private static final List<String> CLASSES = List.of("moose", "bull");
    private static final int WINDOW = 5;

    private final Map<String, Deque<String>> last5 = new HashMap<>();

    @PostMapping("/landmarks")
    public ResultMessage handleLandmarks(@RequestBody String b64,
                                         @RequestHeader(value = "X-Session-Id", required = false) String sid) {
        if (sid == null || sid.isBlank()) sid = UUID.randomUUID().toString();

        byte[] raw = Base64.getDecoder().decode(b64);

        // ----- демонстрационная «ML-модель» -----
        String animal = CLASSES.get(new Random().nextInt(CLASSES.size()));
        float conf = new Random().nextFloat();
        // ----------------------------------------

        Deque<String> q = last5.computeIfAbsent(sid, k -> new ArrayDeque<>());
        q.add(animal);
        boolean finalShot = q.size() == WINDOW;
        if (finalShot) q.clear();

        return new ResultMessage(animal, conf, finalShot);
    }
}
