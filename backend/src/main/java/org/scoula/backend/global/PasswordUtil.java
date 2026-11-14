package org.scoula.backend.global;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {
	private PasswordUtil() {}

	private static byte[] hash(String pw, byte[] salt) {
		try {
			PBEKeySpec spec = new PBEKeySpec(pw.toCharArray(), salt, 65536, 256);
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public static String encode(String pw) {
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash(pw, salt));
	}

	public static boolean matches(String pw, String stored) {
		String[] p = stored.split(":");
		byte[] salt = Base64.getDecoder().decode(p[0]);
		return MessageDigest.isEqual(Base64.getDecoder().decode(p[1]), hash(pw, salt));
	}
}
