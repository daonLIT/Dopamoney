package org.scoula.backend.auth;

import java.util.Map;
import java.util.regex.Pattern;

import org.scoula.backend.global.ApiException;
import org.scoula.backend.global.M;
import org.scoula.backend.global.PasswordUtil;
import org.scoula.backend.global.Team;
import org.scoula.backend.global.TokenStore;
import org.scoula.backend.global.config.AuthInterceptor;
import org.scoula.backend.user.User;
import org.scoula.backend.user.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private static final Pattern LOGIN_ID = Pattern.compile("^[A-Za-z0-9_]{4,20}$");

	private final UserRepository users;
	private final TokenStore tokens;

	public AuthController(UserRepository users, TokenStore tokens) {
		this.users = users;
		this.tokens = tokens;
	}

	public record AuthReq(String loginId, String password) {}

	private Map<String, Object> result(User u) {
		return M.of("token", tokens.issue(u.getId()), "user", M.of("id", u.getId(), "loginId", u.getLoginId(),
				"nickname", u.getNickname(), "teamCode", Team.byCode(u.getTeamCode()) == null ? null : u.getTeamCode()));
	}

	@PostMapping("/signup")
	public ResponseEntity<Map<String, Object>> signup(@RequestBody AuthReq req) {
		if (req.loginId() == null || !LOGIN_ID.matcher(req.loginId()).matches() || req.password() == null
				|| req.password().length() < 8)
			throw new ApiException(400, "아이디는 영문/숫자 4~20자, 비밀번호는 8자 이상이에요");
		if (users.findByLoginId(req.loginId()).isPresent()) throw new ApiException(409, "이미 사용 중인 아이디예요");
		User u = new User();
		u.setLoginId(req.loginId());
		u.setPasswordHash(PasswordUtil.encode(req.password()));
		users.save(u);
		return ResponseEntity.status(201).body(result(u));
	}

	@PostMapping("/login")
	public Map<String, Object> login(@RequestBody AuthReq req) {
		User u = req.loginId() == null || req.password() == null ? null : users.findByLoginId(req.loginId()).orElse(null);
		if (u == null || !PasswordUtil.matches(req.password(), u.getPasswordHash()))
			throw new ApiException(401, "아이디 또는 비밀번호가 맞지 않아요");
		return result(u);
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		tokens.remove(AuthInterceptor.bearer(request));
		return ResponseEntity.noContent().build();
	}
}
