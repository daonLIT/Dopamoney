package org.scoula.backend.leaderboard;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {
	private final LeaderboardService service;

	public LeaderboardController(LeaderboardService service) {
		this.service = service;
	}

	@GetMapping
	public Map<String, Object> board(@RequestAttribute("userId") Long userId,
			@RequestParam(required = false) String nickname, @RequestParam(required = false) Integer limit) {
		return service.board(userId, nickname, limit);
	}
}
