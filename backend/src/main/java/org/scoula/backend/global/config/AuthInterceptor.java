package org.scoula.backend.global.config;

import org.scoula.backend.global.ApiException;
import org.scoula.backend.global.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {
	private final TokenStore tokens;

	public AuthInterceptor(TokenStore tokens) {
		this.tokens = tokens;
	}

	public static String bearer(HttpServletRequest req) {
		String h = req.getHeader("Authorization");
		return h != null && h.startsWith("Bearer ") ? h.substring(7).trim() : null;
	}

	@Override
	public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
		if ("OPTIONS".equals(req.getMethod())) return true;
		String path = req.getRequestURI();
		Long uid = tokens.get(bearer(req));
		if (uid != null) req.setAttribute("userId", uid);
		boolean open = path.startsWith("/api/auth/") || path.equals("/api/teams") || path.equals("/api/health")
				|| ("GET".equals(req.getMethod()) && path.startsWith("/api/schedule"));
		if (!open && uid == null) throw new ApiException(401, "로그인이 필요해요");
		return true;
	}
}
