package ru.gesture.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import ru.gesture.util.CookieUtil;
import ru.gesture.repository.ShotRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HistoryController {

	private final ShotRepository shots;
	private final CookieUtil     cookies;

	public HistoryController(ShotRepository shots, CookieUtil cookies) {
		this.shots   = shots;
		this.cookies = cookies;
	}

	/** Возвращает последние ≤ 100 жестов текущего пользователя */
	@GetMapping("/history")
	public List<Row> history(HttpServletRequest req) {
		return cookies.readUid(req)
				.map(uid -> shots
						.findTop100BySession_User_IdOrderByIdDesc(uid)
						.stream()
						.map(Row::new)
						.toList()
				)
				.orElse(List.of());
	}

	/* компактное DTO для фронта */
	record Row(Long id, String utc, String user, String animal, float conf) {
		Row(ru.gesture.model.Shot s) {
			this(
					s.getId(),
					s.getShotTime().toString(),
					s.getSession().getUser().getName(),
					s.getAnimal(),
					s.getConfidence()
			);
		}
	}
}
