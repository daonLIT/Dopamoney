package org.scoula.backend.record;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scoula.backend.global.ApiException;
import org.scoula.backend.global.M;
import org.scoula.backend.global.Team;
import org.scoula.backend.schedule.ScheduleService;
import org.scoula.backend.user.User;
import org.scoula.backend.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RecordService {
	private static final Set<String> TAGS = Set.of("WIN", "LOSE", "REVERSE_LOSS", "BIG_WIN", "VERIFIED");
	private static final String[] LEVEL_TITLES = { "입문 팬", "새내기", "응원단원", "단골", "열혈 팬", "베테랑", "낭만 수집가", "불굴의 팬",
			"전설의 팬", "명예의 전당" };

	private final GameRecordRepository records;
	private final UserRepository users;
	private final ScheduleService schedule;

	public RecordService(GameRecordRepository records, UserRepository users, ScheduleService schedule) {
		this.records = records;
		this.users = users;
		this.schedule = schedule;
	}

	public record RecordReq(String gameDate, String gameId, Integer ticketCost, Integer transportCost,
			Integer foodCost, String memo, String tag) {}

	// ---------- 계산 ----------

	public static int lossPoint(List<GameRecord> list) {
		long sum = 0;
		for (GameRecord r : list) {
			if (!"LOSE".equals(r.getResult())) continue;
			long p = 5000 + Math.round(r.totalCost() * 0.1);
			if ("REVERSE_LOSS".equals(r.getTag())) p *= 2;
			sum += p;
		}
		return (int) sum;
	}

	public static Map<String, Object> level(int lossPoint) {
		int lv = Math.min(10, 1 + lossPoint / 20000);
		return M.of("lv", lv, "title", LEVEL_TITLES[lv - 1]);
	}

	public static Map<String, Object> summary(List<GameRecord> list) {
		int wins = 0, losses = 0, cost = 0;
		LocalDate last = null;
		for (GameRecord r : list) {
			cost += r.totalCost();
			if ("WIN".equals(r.getResult())) wins++;
			if ("LOSE".equals(r.getResult())) losses++;
			if (last == null || r.getGameDate().isAfter(last)) last = r.getGameDate();
		}
		int winRate = wins + losses == 0 ? 0 : (int) Math.round(wins * 100.0 / (wins + losses));
		return M.of("lossPoint", lossPoint(list), "totalCost", cost, "visitCount", list.size(), "wins", wins,
				"losses", losses, "winRate", winRate, "lastRecordDate", last == null ? null : last.toString());
	}

	public static Map<String, Object> view(GameRecord r) {
		Team opp = Team.byCode(r.getOpponentCode());
		return M.of("id", r.getId(), "gameDate", r.getGameDate().toString(),
				"opponent", opp == null ? null : M.of("code", opp.code, "name", opp.fullName),
				"result", r.getResult(), "tag", r.getTag(), "memo", r.getMemo(),
				"ticketCost", r.getTicketCost(), "transportCost", r.getTransportCost(), "foodCost", r.getFoodCost(),
				"totalCost", r.totalCost(), "myScore", r.getMyScore(), "opponentScore", r.getOpponentScore(),
				"game", gameView(r));
	}

	private static Map<String, Object> gameView(GameRecord r) {
		if (r.getGameId() == null) return null;
		Team a = Team.byCode(r.getAwayCode()), h = Team.byCode(r.getHomeCode());
		return M.of("gameId", r.getGameId(),
				"away", M.of("code", r.getAwayCode(), "name", a == null ? r.getAwayCode() : a.fullName, "score", r.getAwayScore()),
				"home", M.of("code", r.getHomeCode(), "name", h == null ? r.getHomeCode() : h.fullName, "score", r.getHomeScore()));
	}

	// ---------- 조회 ----------

	public List<GameRecord> of(Long userId) {
		return records.findByUserIdOrderByGameDateDescIdDesc(userId);
	}

	public Map<String, Object> summaryOf(Long userId) {
		return summary(of(userId));
	}

	public List<Map<String, Object>> list(Long userId, Integer limit) {
		int n = limit == null || limit < 1 ? 100 : limit;
		return of(userId).stream().limit(n).map(RecordService::view).toList();
	}

	// ---------- 저장 ----------

	public Map<String, Object> save(Long userId, Long recordId, RecordReq req) {
		GameRecord r;
		if (recordId == null) {
			r = new GameRecord();
			r.setUserId(userId);
		} else {
			r = records.findByIdAndUserId(recordId, userId).orElseThrow(() -> new ApiException(404, "기록을 찾을 수 없어요"));
		}
		if (req == null || req.gameDate() == null || req.gameDate().isBlank() || req.memo() == null || req.memo().isBlank())
			throw new ApiException(400, "경기 날짜와 감상은 필수예요");
		LocalDate date;
		try {
			date = LocalDate.parse(req.gameDate());
		} catch (Exception e) {
			throw new ApiException(400, "경기 날짜와 감상은 필수예요");
		}
		String tag = req.tag() == null || req.tag().isBlank() ? null : req.tag();
		if (tag != null && !TAGS.contains(tag)) throw new ApiException(400, "태그가 올바르지 않아요");
		if (req.gameId() == null || req.gameId().isBlank()) throw new ApiException(400, "경기를 선택해주세요");
		// 그 날 실제로 열린 경기인지 일정에서 확인 (일정을 못 불러오면 502)
		Map<String, Object> game = schedule.gamesOfDay(date).stream()
				.filter(g -> req.gameId().equals(g.get("gameId"))).findFirst()
				.orElseThrow(() -> new ApiException(400, "그 날 열린 경기가 아니에요"));
		if ("CANCELED".equals(game.get("status"))) throw new ApiException(400, "취소된 경기는 기록할 수 없어요");

		String away = ScheduleService.code(game, "away"), home = ScheduleService.code(game, "home");
		Integer as = scoreOrNull(game, "away"), hs = scoreOrNull(game, "home");
		User u = users.findById(userId).orElse(null);
		String myTeam = u == null ? null : u.getTeamCode();
		boolean mine = myTeam != null && (myTeam.equals(away) || myTeam.equals(home));

		r.setGameDate(date);
		r.setGameId(req.gameId());
		r.setAwayCode(away);
		r.setHomeCode(home);
		r.setAwayScore(as);
		r.setHomeScore(hs);
		r.setOpponentCode(mine ? (myTeam.equals(away) ? home : away) : null);
		r.setTag(tag);
		r.setMemo(req.memo().length() > 1000 ? req.memo().substring(0, 1000) : req.memo());
		r.setTicketCost(cost(req.ticketCost()));
		r.setTransportCost(cost(req.transportCost()));
		r.setFoodCost(cost(req.foodCost()));

		// 결과: 내 팀 경기가 끝났으면 실제 점수로, 아니면 tag로 판단
		r.setMyScore(null);
		r.setOpponentScore(null);
		String result = null;
		if (mine && as != null && hs != null) {
			int my = myTeam.equals(away) ? as : hs, other = myTeam.equals(away) ? hs : as;
			r.setMyScore(my);
			r.setOpponentScore(other);
			if (my > other) result = "WIN";
			else if (my < other) result = "LOSE";
		}
		if (result == null && r.getMyScore() == null && tag != null) {
			result = switch (tag) {
				case "WIN", "BIG_WIN" -> "WIN";
				case "LOSE", "REVERSE_LOSS" -> "LOSE";
				default -> null;
			};
		}
		r.setResult(result);
		return view(records.save(r));
	}

	@SuppressWarnings("unchecked")
	private static Integer scoreOrNull(Map<String, Object> g, String side) {
		Object v = ((Map<String, Object>) g.get(side)).get("score");
		return v instanceof Number n ? n.intValue() : null;
	}

	private static int cost(Integer v) {
		return v == null || v < 0 ? 0 : v;
	}

	public void delete(Long userId, Long id) {
		GameRecord r = records.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(404, "기록을 찾을 수 없어요"));
		records.delete(r);
	}
}
