package ru.gesture.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Controller;
import ru.gesture.dto.ResultMessage;

@Controller
@RequiredArgsConstructor
public class LandmarkController {

    @MessageMapping("/landmarks")
    @SendTo("/topic/result")
    public ResultMessage handle(byte[] buf) {
        // рандом
        String animal = Math.random() < 0.5 ? "moose" : "bull";
        float  conf   = (float) Math.random();
        return new ResultMessage(animal, conf);
    }
}
