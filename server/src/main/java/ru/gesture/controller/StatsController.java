package ru.gesture.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.gesture.dto.StatRow;
import ru.gesture.repository.ShotRepository;

import java.util.List;

@RestController @RequestMapping("/api")
@RequiredArgsConstructor
public class StatsController {

    private final ShotRepository shots;

    @GetMapping("/stats")
    public List<StatRow> stats() {
        return shots.aggregate();
    }
}
