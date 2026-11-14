package org.scoula.backend.global;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public enum Team {
	LG("LG", "LG 트윈스", "#c30452", "LG", "LG"),
	KT("KT", "KT 위즈", "#000000", "KT", "KT"),
	SSG("SSG", "SSG 랜더스", "#ce0e2d", "SK", "SSG"),
	NC("NC", "NC 다이노스", "#315288", "NC", "NC"),
	DOO("DOO", "두산 베어스", "#131230", "OB", "두산"),
	KIA("KIA", "KIA 타이거즈", "#ea0029", "HT", "KIA"),
	LOT("LOT", "롯데 자이언츠", "#041e42", "LT", "롯데"),
	SAM("SAM", "삼성 라이온즈", "#074ca1", "SS", "삼성"),
	HAN("HAN", "한화 이글스", "#cf3f1f", "HH", "한화"),
	KIW("KIW", "키움 히어로즈", "#570514", "WO", "키움");

	public final String code, fullName, color, kboCode, shortName;

	Team(String code, String fullName, String color, String kboCode, String shortName) {
		this.code = code;
		this.fullName = fullName;
		this.color = color;
		this.kboCode = kboCode;
		this.shortName = shortName;
	}

	public static Team byCode(String code) {
		if (code == null) return null;
		return Arrays.stream(values()).filter(t -> t.code.equals(code)).findFirst().orElse(null);
	}

	public static Team byShortName(String s) {
		if (s == null) return null;
		return Arrays.stream(values()).filter(t -> t.shortName.equals(s.trim())).findFirst().orElse(null);
	}

	public static List<Map<String, Object>> all() {
		return Arrays.stream(values()).map(t -> M.of("code", t.code, "name", t.fullName, "color", t.color)).toList();
	}
}
