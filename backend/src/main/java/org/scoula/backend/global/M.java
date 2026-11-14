package org.scoula.backend.global;

import java.util.LinkedHashMap;
import java.util.Map;

/** null 값을 허용하는 순서 유지 Map 빌더. M.of("a", 1, "b", null) */
public final class M {
	private M() {}

	public static Map<String, Object> of(Object... kv) {
		Map<String, Object> m = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
		return m;
	}
}
