package ru.gesture.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import ru.gesture.dto.StatRow;
import ru.gesture.repository.ShotRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final ShotRepository shots;

    @Autowired
    public StatsController(ShotRepository shots) {
        this.shots = shots;
    }

    @GetMapping("/stats")
    public List<StatRow> stats() {
        return shots.aggregate();
    }
}
