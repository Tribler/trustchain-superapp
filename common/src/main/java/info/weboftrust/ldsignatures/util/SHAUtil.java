package info.weboftrust.ldsignatures.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHAUtil {

	private SHAUtil() {

	}

	public static byte[] sha256(String string) {

		MessageDigest digest;

		try {

			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {

			throw new RuntimeException(ex.getMessage(), ex);
		}

		return digest.digest(string.getBytes(StandardCharsets.UTF_8));
	}
}
