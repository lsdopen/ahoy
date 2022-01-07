package za.co.lsd.ahoy.server.util;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

public final class HashUtil {

	private HashUtil() {
	}

	public static String hash(String s) {
		return Hashing.sha256().hashString(s, StandardCharsets.UTF_8).toString();
	}
}
