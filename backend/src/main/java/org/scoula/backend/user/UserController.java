package org.scoula.backend.user;

import java.util.List;
import java.util.Map;

import org.scoula.backend.global.ApiException;
import org.scoula.backend.global.M;
import org.scoula.backend.global.PasswordUtil;
import org.scoula.backend.global.Team;
import org.scoula.backend.record.GameRecord;
import org.scoula.backend.record.RecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final UserRepository users;
	private final RecordService records;

	public UserController(UserRepository users, RecordService records) {
		this.users = users;
		this.records = records;
	}

	public record ProfileReq(String nickname, String teamCode) {}

	public record PasswordReq(String currentPassword, String newPassword) {}

	private User me(Long id) {
		return users.findById(id).orElseThrow(() -> new ApiException(401, "로그인이 필요해요"));
	}

	public static Map<String, Object> view(User u, List<GameRecord> recs) {
		Team t = Team.byCode(u.getTeamCode());
		return M.of("id", u.getId(), "loginId", u.getLoginId(), "nickname", u.getNickname(),
				"team", t == null ? null : M.of("code", t.code, "name", t.fullName),
				"visitCount", recs.size(), "level", RecordService.level(RecordService.lossPoint(recs)));
	}

	@GetMapping("/me")
	public Map<String, Object> getMe(@RequestAttribute("userId") Long userId) {
		return view(me(userId), records.of(userId));
	}

	@PutMapping("/me/profile")
	public Map<String, Object> profile(@RequestAttribute("userId") Long userId, @RequestBody ProfileReq req) {
		User u = me(userId);
		String nick = req.nickname() == null ? "" : req.nickname().trim();
		if (nick.length() < 2 || nick.length() > 10) throw new ApiException(400, "닉네임은 2~10자예요");
		if (Team.byCode(req.teamCode()) == null) throw new ApiException(400, "응원팀을 선택해 주세요");
		users.findByNickname(nick).ifPresent(o -> {
			if (!o.getId().equals(userId)) throw new ApiException(409, "이미 사용 중인 닉네임이에요");
		});
		u.setNickname(nick);
		u.setTeamCode(req.teamCode());
		users.save(u);
		return view(u, records.of(userId));
	}

	@PatchMapping("/me/password")
	public ResponseEntity<Void> password(@RequestAttribute("userId") Long userId, @RequestBody PasswordReq req) {
		User u = me(userId);
		if (req.currentPassword() == null || !PasswordUtil.matches(req.currentPassword(), u.getPasswordHash()))
			throw new ApiException(400, "현재 비밀번호가 맞지 않아요");
		if (req.newPassword() == null || req.newPassword().length() < 8)
			throw new ApiException(400, "비밀번호는 8자 이상이에요");
		u.setPasswordHash(PasswordUtil.encode(req.newPassword()));
		users.save(u);
		return ResponseEntity.noContent().build();
	}
}
