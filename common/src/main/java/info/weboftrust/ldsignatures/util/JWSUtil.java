package info.weboftrust.ldsignatures.util;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.util.Base64URL;

import java.nio.charset.StandardCharsets;

public class JWSUtil {

	private JWSUtil() {

	}

	public static byte[] getJwsSigningInput(JWSHeader header, byte[] signingInput) {

		byte[] encodedHeader;

		if (header.getParsedBase64URL() != null)
			encodedHeader = header.getParsedBase64URL().toString().getBytes(StandardCharsets.UTF_8);
		else
			encodedHeader = header.toBase64URL().toString().getBytes(StandardCharsets.UTF_8);

		byte[] jwsSigningInput = new byte[encodedHeader.length + 1 + signingInput.length];
		System.arraycopy(encodedHeader, 0, jwsSigningInput, 0, encodedHeader.length);
		jwsSigningInput[encodedHeader.length] = (byte) '.';
		System.arraycopy(signingInput, 0, jwsSigningInput, encodedHeader.length+1, signingInput.length);

		return jwsSigningInput;
	}

	public static String serializeDetachedJws(JWSHeader jwsHeader, Base64URL signature) {

		return jwsHeader.toBase64URL().toString() + '.' + '.' + signature.toString();
	}
}
