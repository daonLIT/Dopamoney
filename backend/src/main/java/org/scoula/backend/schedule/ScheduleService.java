package org.scoula.backend.schedule;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scoula.backend.global.ApiException;
import org.scoula.backend.global.M;
import org.scoula.backend.global.Team;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ScheduleService {
	private static final String BASE = "https://www.koreabaseball.com/ws/Schedule.asmx/";
	private static final String REFERER = "https://www.koreabaseball.com/Schedule/Schedule.aspx";
	public static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/** srId -> {stage, stageText} */
	private static final Map<Integer, String[]> STAGES = new LinkedHashMap<>();
	static {
		STAGES.put(0, new String[] { "REGULAR", null });
		STAGES.put(1, new String[] { "PRESEASON", "시범경기" });
		STAGES.put(3, new String[] { "SEMI_PO", "준플레이오프" });
		STAGES.put(4, new String[] { "WC", "와일드카드" });
		STAGES.put(5, new String[] { "PO", "플레이오프" });
		STAGES.put(7, new String[] { "KS", "한국시리즈" });
		STAGES.put(9, new String[] { "ALLSTAR", "올스타전" });
	}

	private final ObjectMapper mapper;
	private final Path dir;
	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	public ScheduleService(ObjectMapper mapper, @Value("${SCHEDULE_CACHE_DIR:data/schedule}") String dir) {
		this.mapper = mapper;
		this.dir = Path.of(dir);
	}

	// ---------- 공개 API ----------

	public static LocalDate today() {
		return LocalDate.now(KST);
	}

	/** 그 날의 경기들(myTeamCode 경기 먼저, 시간순). */
	public Map<String, Object> day(LocalDate date, String myTeamCode) {
		List<Map<String, Object>> games = new ArrayList<>();
		for (Map<String, Object> g : gamesOfDay(date)) {
			Map<String, Object> copy = new LinkedHashMap<>(g);
			copy.remove("date");
			boolean mine = myTeamCode != null && (myTeamCode.equals(code(g, "away")) || myTeamCode.equals(code(g, "home")));
			copy.put("myTeam", mine);
			games.add(copy);
		}
		games.sort(Comparator.comparing((Map<String, Object> g) -> !(Boolean) g.get("myTeam"))
				.thenComparing(g -> String.valueOf(g.get("time"))));
		return M.of("date", date.toString(), "games", games);
	}

	public Map<String, Object> month(YearMonth ym) {
		Map<String, Object> f = load(ym);
		return M.of("month", ym.toString(), "days", f.get("calendar"));
	}

	/** 날짜의 원본(date 포함) 경기들. 실패 시 ApiException(502). */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> gamesOfDay(LocalDate date) {
		Map<String, Object> f = load(YearMonth.from(date));
		List<Map<String, Object>> all = (List<Map<String, Object>>) f.get("games");
		return all.stream().filter(g -> date.toString().equals(g.get("date"))).toList();
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> gamesOfMonth(YearMonth ym) {
		return (List<Map<String, Object>>) load(ym).get("games");
	}

	@SuppressWarnings("unchecked")
	public static String code(Map<String, Object> g, String side) {
		return (String) ((Map<String, Object>) g.get(side)).get("code");
	}

	// ---------- 캐시 ----------

	@SuppressWarnings("unchecked")
	private synchronized Map<String, Object> load(YearMonth ym) {
		Path file = dir.resolve(ym + ".json");
		Map<String, Object> cached = null;
		try {
			if (Files.exists(file)) cached = mapper.readValue(file.toFile(), Map.class);
		} catch (Exception e) {
			cached = null;
		}
		if (cached != null) {
			boolean past = ym.isBefore(YearMonth.now(KST));
			boolean fresh = past;
			if (!fresh) {
				try {
					fresh = Instant.parse((String) cached.get("fetchedAt")).isAfter(Instant.now().minus(Duration.ofMinutes(10)));
				} catch (Exception ignore) {
				}
			}
			if (fresh) return cached;
		}
		try {
			Map<String, Object> fresh = fetch(ym);
			Files.createDirectories(dir);
			Path tmp = dir.resolve(ym + ".json.tmp");
			mapper.writeValue(tmp.toFile(), fresh);
			Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			return fresh;
		} catch (Exception e) {
			System.err.println("KBO 일정 호출 실패 " + ym + ": " + e);
			if (cached != null) return cached;
			throw new ApiException(502, "경기 일정을 불러오지 못했어요");
		}
	}

	/** 시작 시 이번 달/지난달 미리 받기 */
	public void preload() {
		YearMonth now = YearMonth.now(KST);
		for (YearMonth ym : new YearMonth[] { now, now.minusMonths(1) }) {
			try {
				load(ym);
			} catch (Exception e) {
				System.err.println("일정 미리받기 실패 " + ym);
			}
		}
	}

	// ---------- KBO 호출 ----------

	private String post(String method, String form) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create(BASE + method))
				.timeout(Duration.ofSeconds(15))
				.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
				.header("X-Requested-With", "XMLHttpRequest")
				.header("Referer", REFERER)
				.header("User-Agent", "Mozilla/5.0")
				.POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8)).build();
		HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (res.statusCode() != 200) throw new IllegalStateException("KBO status " + res.statusCode());
		String body = res.body();
		if (body == null || !body.trim().startsWith("{")) throw new IllegalStateException("KBO non-json response");
		return body;
	}

	private Map<String, Object> fetch(YearMonth ym) throws Exception {
		String mm = String.format("%02d", ym.getMonthValue());
		List<Map<String, Object>> games = new ArrayList<>();
		for (Map.Entry<Integer, String[]> e : STAGES.entrySet()) {
			String body = post("GetScheduleList",
					"leId=1&srIdList=" + e.getKey() + "&seasonId=" + ym.getYear() + "&gameMonth=" + mm + "&teamId=");
			games.addAll(parseList(mapper.readTree(body), ym, e.getValue()));
		}
		games.sort(Comparator.comparing((Map<String, Object> g) -> (String) g.get("date"))
				.thenComparing(g -> String.valueOf(g.get("time"))));
		String calBody = post("GetMonthSchedule",
				"leId=1&srIdList=0,1,3,4,5,7,9&seasonId=" + ym.getYear() + "&gameMonth=" + mm);
		List<Map<String, Object>> calendar = parseCalendar(mapper.readTree(calBody), ym, games);
		return M.of("fetchedAt", Instant.now().toString(), "games", games, "calendar", calendar);
	}

	// ---------- 파싱 ----------

	private static String strip(String html) {
		if (html == null) return "";
		return html.replaceAll("<br\\s*/?>", " ").replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
	}

	private static String text(JsonNode n, String field) {
		JsonNode v = n.get(field);
		return v == null || v.isNull() ? null : v.asText();
	}

	private static final Pattern PLAY = Pattern.compile(
			"<span[^>]*>([^<]*)</span>\\s*<em>(.*?)</em>\\s*<span[^>]*>([^<]*)</span>", Pattern.DOTALL);
	private static final Pattern SCORE = Pattern.compile("<span[^>]*>(\\d+)</span>");
	private static final Pattern GAME_ID = Pattern.compile("gameId=(\\w+)");
	private static final Pattern DAY = Pattern.compile("(\\d{2})\\.(\\d{2})");

	private static Map<String, Object> teamObj(String shortName, Integer score) {
		Team t = Team.byShortName(shortName);
		return M.of("code", t != null ? t.code : shortName, "name", t != null ? t.fullName : shortName, "score", score);
	}

	private static String kbo(String shortName) {
		Team t = Team.byShortName(shortName);
		return t != null ? t.kboCode : shortName;
	}

	private List<Map<String, Object>> parseList(JsonNode root, YearMonth ym, String[] stage) {
		List<Map<String, Object>> out = new ArrayList<>();
		JsonNode rows = root.get("rows");
		if (rows == null || !rows.isArray()) return out;
		LocalDate curDate = null;
		for (JsonNode r : rows) {
			List<JsonNode> cells = new ArrayList<>();
			for (JsonNode c : r.get("row")) cells.add(c);
			int i = 0;
			if (!cells.isEmpty() && "day".equals(text(cells.get(0), "Class"))) {
				Matcher dm = DAY.matcher(strip(text(cells.get(0), "Text")));
				if (dm.find()) curDate = LocalDate.of(ym.getYear(), Integer.parseInt(dm.group(1)), Integer.parseInt(dm.group(2)));
				i = 1;
			}
			List<JsonNode> rest = cells.subList(i, cells.size());
			JsonNode play = null, time = null;
			for (JsonNode c : rest) {
				if ("play".equals(text(c, "Class"))) play = c;
				if ("time".equals(text(c, "Class"))) time = c;
			}
			if (play == null || curDate == null) continue; // 이동일 등
			Matcher pm = PLAY.matcher(text(play, "Text"));
			if (!pm.find()) continue;
			String away = pm.group(1).trim(), home = pm.group(3).trim();
			List<Integer> scores = new ArrayList<>();
			Matcher sm = SCORE.matcher(pm.group(2));
			while (sm.find()) scores.add(Integer.parseInt(sm.group(1)));
			String stadium = rest.size() >= 2 ? strip(text(rest.get(rest.size() - 2), "Text")) : null;
			String remark = rest.isEmpty() ? "" : strip(text(rest.get(rest.size() - 1), "Text"));
			String timeStr = time == null ? null : strip(text(time, "Text"));
			String gameId = null;
			for (JsonNode c : rest) {
				Matcher gm = GAME_ID.matcher(String.valueOf(text(c, "Text")));
				if (gm.find()) {
					gameId = gm.group(1);
					break;
				}
			}
			String date = curDate.toString();
			if (gameId == null) gameId = curDate.toString().replace("-", "") + kbo(away) + kbo(home) + "0";

			String status, statusText;
			Integer as = null, hs = null;
			if (remark.contains("취소")) {
				status = "CANCELED";
				statusText = remark.contains("우천") ? "우천 취소" : "경기 취소";
			} else if (scores.size() >= 2) {
				status = "FINISHED";
				statusText = "종료";
				as = scores.get(0);
				hs = scores.get(1);
			} else {
				status = "SCHEDULED";
				statusText = "경기 전";
			}
			out.add(M.of("date", date, "gameId", gameId, "stage", stage[0], "stageText", stage[1],
					"stadium", stadium, "time", timeStr, "status", status, "statusText", statusText,
					"away", teamObj(away, as), "home", teamObj(home, hs)));
		}
		return out;
	}

	private static final Pattern DAYNUM = Pattern.compile("<li class=\"dayNum\">(\\d*)</li>");

	private List<Map<String, Object>> parseCalendar(JsonNode root, YearMonth ym, List<Map<String, Object>> games) {
		Set<String> canceledDates = new LinkedHashSet<>();
		for (Map<String, Object> g : games) if ("CANCELED".equals(g.get("status"))) canceledDates.add((String) g.get("date"));
		TreeMap<String, Map<String, Object>> days = new TreeMap<>();
		JsonNode rows = root.get("rows");
		if (rows != null && rows.isArray()) {
			for (JsonNode r : rows) {
				for (JsonNode c : r.get("row")) {
					String t = String.valueOf(text(c, "Text"));
					Matcher m = DAYNUM.matcher(t);
					if (!m.find() || m.group(1).isEmpty()) continue;
					String date = ym.atDay(Integer.parseInt(m.group(1))).toString();
					Set<String> ids = new LinkedHashSet<>();
					Matcher gm = GAME_ID.matcher(t);
					while (gm.find()) ids.add(gm.group(1));
					boolean canceled = canceledDates.contains(date);
					if (ids.isEmpty() && !canceled) continue;
					Map<String, Object> d = M.of("date", date, "games", ids.size());
					if (canceled) d.put("canceled", true);
					days.put(date, d);
				}
			}
		}
		for (String date : canceledDates) days.computeIfAbsent(date, k -> M.of("date", k, "games", 0, "canceled", true));
		return new ArrayList<>(days.values());
	}
}
