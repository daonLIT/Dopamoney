package org.scoula.backend.global;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class TokenStore {
	private final Map<String, Long> tokens = new ConcurrentHashMap<>();

	public String issue(Long userId) {
		String t = UUID.randomUUID().toString().replace("-", "");
		tokens.put(t, userId);
		return t;
	}

	public Long get(String token) {
		return token == null ? null : tokens.get(token);
	}

	public void remove(String token) {
		if (token != null) tokens.remove(token);
	}
}
