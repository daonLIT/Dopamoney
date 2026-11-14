package org.scoula.backend.leaderboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scoula.backend.global.M;
import org.scoula.backend.global.Team;
import org.scoula.backend.record.GameRecord;
import org.scoula.backend.record.GameRecordRepository;
import org.scoula.backend.record.RecordService;
import org.scoula.backend.user.User;
import org.scoula.backend.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {
	private final UserRepository users;
	private final GameRecordRepository records;

	public LeaderboardService(UserRepository users, GameRecordRepository records) {
		this.users = users;
		this.records = records;
	}

	private record Row(User user, int lossPoint, int visit) {}

	private Map<String, Object> item(int rank, Row r, boolean me) {
		Team t = Team.byCode(r.user.getTeamCode());
		return M.of("rank", rank, "nickname", r.user.getNickname(), "teamName", t == null ? null : t.fullName,
				"visitCount", r.visit, "lossPoint", r.lossPoint, "me", me);
	}

	public Map<String, Object> board(Long meId, String nickname, Integer limit) {
		Map<Long, List<GameRecord>> byUser = new HashMap<>();
		for (GameRecord r : records.findAll()) byUser.computeIfAbsent(r.getUserId(), k -> new ArrayList<>()).add(r);
		List<Row> rows = new ArrayList<>();
		Row meRow = null;
		for (User u : users.findAll()) {
			List<GameRecord> l = byUser.getOrDefault(u.getId(), List.of());
			Row row = new Row(u, RecordService.lossPoint(l), l.size());
			if (u.getNickname() != null) rows.add(row);
			if (u.getId().equals(meId)) meRow = row;
		}
		rows.sort(Comparator.comparingInt((Row r) -> -r.lossPoint).thenComparingInt(r -> -r.visit)
				.thenComparing(r -> r.user.getId()));
		int n = limit == null || limit < 1 ? 50 : limit;
		String q = nickname == null ? "" : nickname.trim();
		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> me = null;
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			boolean isMe = r.user.getId().equals(meId);
			if (isMe) me = item(i + 1, r, true);
			if (!q.isEmpty() && !r.user.getNickname().contains(q)) continue;
			if (items.size() < n) items.add(item(i + 1, r, isMe));
		}
		if (me == null && meRow != null) me = item(rows.size() + 1, meRow, true); // 닉네임 미설정 유저
		return M.of("items", items, "me", me);
	}
}
