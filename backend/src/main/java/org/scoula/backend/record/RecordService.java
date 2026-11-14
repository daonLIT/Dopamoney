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

	public record RecordReq(String gameDate, String opponentCode, Integer ticketCost, Integer transportCost,
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
				"totalCost", r.totalCost(), "myScore", r.getMyScore(), "opponentScore", r.getOpponentScore());
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
		String opp = req.opponentCode() == null || req.opponentCode().isBlank() ? null : req.opponentCode();
		if (opp != null && Team.byCode(opp) == null) throw new ApiException(400, "상대 팀이 올바르지 않아요");

		r.setGameDate(date);
		r.setOpponentCode(opp);
		r.setTag(tag);
		r.setMemo(req.memo().length() > 1000 ? req.memo().substring(0, 1000) : req.memo());
		r.setTicketCost(cost(req.ticketCost()));
		r.setTransportCost(cost(req.transportCost()));
		r.setFoodCost(cost(req.foodCost()));

		// 결과: 스케줄 캐시 -> tag -> null
		r.setMyScore(null);
		r.setOpponentScore(null);
		String result = null;
		User u = users.findById(userId).orElse(null);
		String myTeam = u == null ? null : u.getTeamCode();
		if (myTeam != null && opp != null) {
			try {
				for (Map<String, Object> g : schedule.gamesOfDay(date)) {
					String a = ScheduleService.code(g, "away"), h = ScheduleService.code(g, "home");
					boolean match = (a.equals(myTeam) && h.equals(opp)) || (h.equals(myTeam) && a.equals(opp));
					if (!match || !"FINISHED".equals(g.get("status"))) continue;
					int as = score(g, "away"), hs = score(g, "home");
					int my = a.equals(myTeam) ? as : hs, other = a.equals(myTeam) ? hs : as;
					r.setMyScore(my);
					r.setOpponentScore(other);
					if (my > other) result = "WIN";
					else if (my < other) result = "LOSE";
					break;
				}
			} catch (Exception e) {
				// 스케줄을 못 불러오면 tag로 판단
			}
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
	private static int score(Map<String, Object> g, String side) {
		return ((Number) ((Map<String, Object>) g.get(side)).get("score")).intValue();
	}

	private static int cost(Integer v) {
		return v == null || v < 0 ? 0 : v;
	}

	public void delete(Long userId, Long id) {
		GameRecord r = records.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(404, "기록을 찾을 수 없어요"));
		records.delete(r);
	}
}
