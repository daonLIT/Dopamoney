package org.scoula.backend.team;

import java.util.List;
import java.util.Map;

import org.scoula.backend.global.Team;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TeamController {

	@GetMapping("/api/teams")
	public List<Map<String, Object>> teams() {
		return Team.all();
	}
}
