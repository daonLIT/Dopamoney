package org.scoula.backend.home;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.scoula.backend.global.ApiException;
import org.scoula.backend.global.M;
import org.scoula.backend.global.Team;
import org.scoula.backend.leaderboard.LeaderboardService;
import org.scoula.backend.record.GameRecord;
import org.scoula.backend.record.RecordService;
import org.scoula.backend.schedule.ScheduleService;
import org.scoula.backend.user.User;
import org.scoula.backend.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {
	private final UserRepository users;
	private final RecordService records;
	private final LeaderboardService leaderboard;
	private final ScheduleService schedule;

	public HomeController(UserRepository users, RecordService records, LeaderboardService leaderboard,
			ScheduleService schedule) {
		this.users = users;
		this.records = records;
		this.leaderboard = leaderboard;
		this.schedule = schedule;
	}

	@GetMapping
	@SuppressWarnings("unchecked")
	public Map<String, Object> home(@RequestAttribute("userId") Long userId) {
		User u = users.findById(userId).orElseThrow(() -> new ApiException(401, "로그인이 필요해요"));
		List<GameRecord> recs = records.of(userId);
		Map<String, Object> sum = RecordService.summary(recs);
		Team team = Team.byCode(u.getTeamCode());
		int lp = (Integer) sum.get("lossPoint");

		Map<String, Object> report = M.of("teamName", team == null ? null : team.fullName, "nickname", u.getNickname(),
				"lossPoint", lp, "level", RecordService.level(lp));
		Map<String, Object> season = M.of("totalCost", sum.get("totalCost"), "visitCount", sum.get("visitCount"),
				"wins", sum.get("wins"), "losses", sum.get("losses"), "winRate", sum.get("winRate"));
		List<Map<String, Object>> top10 = new ArrayList<>();
		for (Map<String, Object> it : (List<Map<String, Object>>) leaderboard.board(userId, null, 10).get("items"))
			top10.add(M.of("rank", it.get("rank"), "nickname", it.get("nickname"), "teamName", it.get("teamName"),
					"lossPoint", it.get("lossPoint"), "me", it.get("me")));
		return M.of("report", report, "season", season, "top10", top10, "recentGame", recentGame(team));
	}

	/** 응원팀의 가장 최근 종료 경기 (최근 3개월 탐색). 실패/없음이면 null. */
	private Map<String, Object> recentGame(Team team) {
		if (team == null) return null;
		LocalDate today = ScheduleService.today();
		try {
			for (int m = 0; m < 3; m++) {
				LocalDate first = today.minusMonths(m).withDayOfMonth(1);
				List<Map<String, Object>> found = new ArrayList<>();
				for (Map<String, Object> g : schedule.gamesOfMonth(java.time.YearMonth.from(first))) {
					if ("FINISHED".equals(g.get("status")) && !LocalDate.parse((String) g.get("date")).isAfter(today)
							&& (team.code.equals(ScheduleService.code(g, "away")) || team.code.equals(ScheduleService.code(g, "home"))))
						found.add(g);
				}
				if (!found.isEmpty()) {
					found.sort(Comparator.comparing((Map<String, Object> g) -> (String) g.get("date"))
							.thenComparing(g -> String.valueOf(g.get("time"))));
					Map<String, Object> g = found.get(found.size() - 1);
					return M.of("date", g.get("date"), "stadium", g.get("stadium"), "status", g.get("status"),
							"away", g.get("away"), "home", g.get("home"));
				}
			}
		} catch (Exception e) {
			// 일정 실패 시 null
		}
		return null;
	}
}
