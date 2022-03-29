package com.danubetech.keyformats;

//import bbs.signatures.KeyPair;
import com.danubetech.keyformats.jose.Curve;
import com.danubetech.keyformats.jose.JWK;
import com.danubetech.keyformats.jose.KeyType;
//import org.apache.commons.codec.binary.Base64;
//import org.apache.commons.codec.binary.Hex;
import org.bitcoinj.core.ECKey;
import org.bouncycastle.math.ec.ECPoint;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import io.ipfs.multibase.binary.Base64;

public class PrivateKey_to_JWK {

	public static JWK RSAPrivateKey_to_JWK(RSAPrivateKey privateKey, RSAPublicKey publicKey, String kid, String use) {

		JWK jsonWebKey = new JWK();
		jsonWebKey.setKty(KeyType.RSA);
		jsonWebKey.setKid(kid);
		jsonWebKey.setUse(use);
		jsonWebKey.setN(Base64.encodeBase64URLSafeString(privateKey.getModulus().toByteArray()));
		jsonWebKey.setD(Base64.encodeBase64URLSafeString(privateKey.getPrivateExponent().toByteArray()));
		jsonWebKey.setE(Base64.encodeBase64URLSafeString(publicKey.getPublicExponent().toByteArray()));

		return jsonWebKey;
	}

	public static JWK RSAPrivateKeyBytes_to_JWK(byte[] privateKeyBytes, byte[] publicKeyBytes, String kid, String use) {

		RSAPrivateKey privateKey;
		RSAPublicKey publicKey;
		try {
			privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
			publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}

		return RSAPrivateKey_to_JWK(privateKey, publicKey, kid, use);
	}

    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

	public static JWK secp256k1PrivateKey_to_JWK(ECKey privateKey, String kid, String use) {

		ECPoint publicKeyPoint = privateKey.getPubKeyPoint();
		byte[] privateKeyBytes = privateKey.getPrivKeyBytes();

		if (publicKeyPoint.getAffineXCoord().getEncoded().length != 32) throw new IllegalArgumentException("Invalid 'x' value (not 32 bytes): " + bytesToHex(publicKeyPoint.getAffineXCoord().getEncoded()) + ", length=" + publicKeyPoint.getAffineXCoord().getEncoded().length);
		if (publicKeyPoint.getAffineYCoord().getEncoded().length != 32) throw new IllegalArgumentException("Invalid 'y' value (not 32 bytes): " + bytesToHex(publicKeyPoint.getAffineYCoord().getEncoded()) + ", length=" + publicKeyPoint.getAffineYCoord().getEncoded().length);
		if (privateKeyBytes.length != 32) throw new IllegalArgumentException("Invalid 'd' value (not 32 bytes): length=" + privateKeyBytes.length);

		JWK jsonWebKey = new JWK();
		jsonWebKey.setKty(KeyType.EC);
		jsonWebKey.setCrv(Curve.secp256k1);
		jsonWebKey.setKid(kid);
		jsonWebKey.setUse(use);
		jsonWebKey.setX(Base64.encodeBase64URLSafeString(publicKeyPoint.getAffineXCoord().getEncoded()));
		jsonWebKey.setY(Base64.encodeBase64URLSafeString(publicKeyPoint.getAffineYCoord().getEncoded()));
		jsonWebKey.setD(Base64.encodeBase64URLSafeString(privateKeyBytes));

		return jsonWebKey;
	}

	public static JWK secp256k1PrivateKeyBytes_to_JWK(byte[] privateKeyBytes, String kid, String use) {

		ECKey privateKey = ECKey.fromPrivate(privateKeyBytes);

		return secp256k1PrivateKey_to_JWK(privateKey, kid, use);
	}

	/*
	public static JWK Bls12381G1PrivateKey_to_JWK(KeyPair privateKey, String kid, String use) {

		byte[] publicKeyBytes = privateKey.publicKey;
		byte[] privateKeyBytes = privateKey.secretKey;

		JWK jsonWebKey = new JWK();
		jsonWebKey.setKty(KeyType.OKP);
		jsonWebKey.setCrv(Curve.Bls12381G1);
		jsonWebKey.setKid(kid);
		jsonWebKey.setUse(use);
		jsonWebKey.setX(Base64.encodeBase64URLSafeString(publicKeyBytes));
		jsonWebKey.setD(Base64.encodeBase64URLSafeString(privateKeyBytes));

		return jsonWebKey;
	}

	public static JWK Bls12381G1PrivateKeyBytes_to_JWK(byte[] privateKeyBytes, byte[] publicKeyBytes, String kid, String use) {

		KeyPair privateKey = new KeyPair(publicKeyBytes, privateKeyBytes);

		return Bls12381G1PrivateKey_to_JWK(privateKey, kid, use);
	}

	public static JWK Bls12381G2PrivateKey_to_JWK(KeyPair privateKey, String kid, String use) {

		byte[] publicKeyBytes = privateKey.publicKey;
		byte[] privateKeyBytes = privateKey.secretKey;

		JWK jsonWebKey = new JWK();
		jsonWebKey.setKty(KeyType.OKP);
		jsonWebKey.setCrv(Curve.Bls12381G2);
		jsonWebKey.setKid(kid);
		jsonWebKey.setUse(use);
		jsonWebKey.setX(Base64.encodeBase64URLSafeString(publicKeyBytes));
		jsonWebKey.setD(Base64.encodeBase64URLSafeString(privateKeyBytes));

		return jsonWebKey;
	}

	public static JWK Bls12381G2PrivateKeyBytes_to_JWK(byte[] privateKeyBytes, byte[] publicKeyBytes, String kid, String use) {

		KeyPair privateKey = new KeyPair(publicKeyBytes, privateKeyBytes);

		return Bls12381G2PrivateKey_to_JWK(privateKey, kid, use);
	}

	public static JWK Ed25519PrivateKeyBytes_to_JWK(byte[] privateKeyBytes, byte[] publicKeyBytes, String kid, String use) {

		byte[] onlyPrivateKeyBytes = Arrays.copyOf(privateKeyBytes, 32);

		JWK jsonWebKey = new JWK();
		jsonWebKey.setKty(KeyType.OKP);
		jsonWebKey.setCrv(Curve.Ed25519);
		jsonWebKey.setKid(kid);
		jsonWebKey.setUse(use);
		jsonWebKey.setX(Base64.encodeBase64URLSafeString(publicKeyBytes));
		jsonWebKey.setD(Base64.encodeBase64URLSafeString(onlyPrivateKeyBytes));

		return jsonWebKey;
	}

	public static JWK X25519PrivateKeyBytes_to_JWK(byte[] privateKeyBytes, byte[] publicKeyBytes, String kid, String use) {

		byte[] onlyPrivateKeyBytes = Arrays.copyOf(privateKeyBytes, 32);

		JWK jsonWebKey = new JWK();
		jsonWebKey.setKty(KeyType.OKP);
		jsonWebKey.setCrv(Curve.X25519);
		jsonWebKey.setKid(kid);
		jsonWebKey.setUse(use);
		jsonWebKey.setX(Base64.encodeBase64URLSafeString(publicKeyBytes));
		jsonWebKey.setD(Base64.encodeBase64URLSafeString(onlyPrivateKeyBytes));

		return jsonWebKey;
	}
	 */

	public static JWK P_256PrivateKey_to_JWK(ECKey privateKey, String kid, String use) {

		ECPoint publicKeyPoint = privateKey.getPubKeyPoint();
		byte[] privateKeyBytes = privateKey.getPrivKeyBytes();

		if (publicKeyPoint.getAffineXCoord().getEncoded().length != 32) throw new IllegalArgumentException("Invalid 'x' value (not 32 bytes): " + bytesToHex(publicKeyPoint.getAffineXCoord().getEncoded()) + ", length=" + publicKeyPoint.getAffineXCoord().getEncoded().length);
		if (publicKeyPoint.getAffineYCoord().getEncoded().length != 32) throw new IllegalArgumentException("Invalid 'y' value (not 32 bytes): " + bytesToHex(publicKeyPoint.getAffineYCoord().getEncoded()) + ", length=" + publicKeyPoint.getAffineYCoord().getEncoded().length);
		if (privateKeyBytes.length != 32) throw new IllegalArgumentException("Invalid 'd' value (not 32 bytes): length=" + privateKeyBytes.length);

		JWK jsonWebKey = new JWK();
		jsonWebKey.setKty(KeyType.EC);
		jsonWebKey.setCrv(Curve.P_256);
		jsonWebKey.setKid(kid);
		jsonWebKey.setUse(use);
		jsonWebKey.setX(Base64.encodeBase64URLSafeString(publicKeyPoint.getAffineXCoord().getEncoded()));
		jsonWebKey.setY(Base64.encodeBase64URLSafeString(publicKeyPoint.getAffineYCoord().getEncoded()));
		jsonWebKey.setD(Base64.encodeBase64URLSafeString(privateKeyBytes));

		return jsonWebKey;
	}

	public static JWK P_256PrivateKeyBytes_to_JWK(byte[] privateKeyBytes, String kid, String use) {

		ECKey privateKey = ECKey.fromPrivate(privateKeyBytes);

		return P_256PrivateKey_to_JWK(privateKey, kid, use);
	}

	public static JWK P_384PrivateKey_to_JWK(ECKey privateKey, String kid, String use) {

		ECPoint publicKeyPoint = privateKey.getPubKeyPoint();
		byte[] privateKeyBytes = privateKey.getPrivKeyBytes();

		if (publicKeyPoint.getAffineXCoord().getEncoded().length != 48) throw new IllegalArgumentException("Invalid 'x' value (not 48 bytes): " + bytesToHex(publicKeyPoint.getAffineXCoord().getEncoded()) + ", length=" + publicKeyPoint.getAffineXCoord().getEncoded().length);
		if (publicKeyPoint.getAffineYCoord().getEncoded().length != 48) throw new IllegalArgumentException("Invalid 'y' value (not 48 bytes): " + bytesToHex(publicKeyPoint.getAffineYCoord().getEncoded()) + ", length=" + publicKeyPoint.getAffineYCoord().getEncoded().length);
		if (privateKeyBytes.length != 48) throw new IllegalArgumentException("Invalid 'd' value (not 48 bytes): length=" + privateKeyBytes.length);

		JWK jsonWebKey = new JWK();
		jsonWebKey.setKty(KeyType.EC);
		jsonWebKey.setCrv(Curve.P_384);
		jsonWebKey.setKid(kid);
		jsonWebKey.setUse(use);
		jsonWebKey.setX(Base64.encodeBase64URLSafeString(publicKeyPoint.getAffineXCoord().getEncoded()));
		jsonWebKey.setY(Base64.encodeBase64URLSafeString(publicKeyPoint.getAffineYCoord().getEncoded()));
		jsonWebKey.setD(Base64.encodeBase64URLSafeString(privateKeyBytes));

		return jsonWebKey;
	}

	public static JWK P_384PrivateKeyBytes_to_JWK(byte[] privateKeyBytes, String kid, String use) {

		ECKey privateKey = ECKey.fromPrivate(privateKeyBytes);

		return P_384PrivateKey_to_JWK(privateKey, kid, use);
	}
}
