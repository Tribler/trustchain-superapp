package com.danubetech.keyformats;

//import bbs.signatures.KeyPair;
import com.danubetech.keyformats.jose.Curve;
import com.danubetech.keyformats.jose.JWK;
import com.danubetech.keyformats.jose.KeyType;
import com.danubetech.keyformats.jose.KeyTypeName;
import com.danubetech.keyformats.keytypes.KeyTypeName_for_JWK;
import org.bitcoinj.core.ECKey;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateKeySpec;

public class JWK_to_PrivateKey {

	public static Object JWK_to_anyPrivateKey(JWK jsonWebKey) {

		KeyTypeName keyType = KeyTypeName_for_JWK.keyTypeName_for_JWK(jsonWebKey);

		if (keyType == KeyTypeName.RSA)
			return JWK_to_RSAPrivateKey(jsonWebKey);
		else if (keyType == KeyTypeName.secp256k1)
			return JWK_to_secp256k1PrivateKey(jsonWebKey);
//		else if (keyType == KeyTypeName.Bls12381G1)
//			return JWK_to_Bls12381G1PrivateKey(jsonWebKey);
//		else if (keyType == KeyTypeName.Bls12381G2)
//			return JWK_to_Bls12381G2PrivateKey(jsonWebKey);
//		else if (keyType == KeyTypeName.Bls48581G1)
//			return JWK_to_Bls12381G1PrivateKey(jsonWebKey);
//		else if (keyType == KeyTypeName.Bls48581G2)
//			return JWK_to_Bls12381G2PrivateKey(jsonWebKey);
//		else if (keyType == KeyTypeName.Ed25519)
//			return JWK_to_Ed25519PrivateKeyBytes(jsonWebKey);
//		else if (keyType == KeyTypeName.X25519)
//			return JWK_to_X25519PrivateKeyBytes(jsonWebKey);
		else if (keyType == KeyTypeName.P_256)
			return JWK_to_P_256PrivateKeyBytes(jsonWebKey);
		else if (keyType == KeyTypeName.P_384)
			return JWK_to_P_384PrivateKeyBytes(jsonWebKey);
		else
			throw new IllegalArgumentException("Unsupported key type: " + keyType);
	}

	public static RSAPrivateKey JWK_to_RSAPrivateKey(JWK jsonWebKey) {

		if (! KeyType.RSA.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAPrivateKeySpec rsaPrivateKeySpec = new RSAPrivateKeySpec(new BigInteger(jsonWebKey.getNdecoded()), new BigInteger(jsonWebKey.getDdecoded()));
			return (RSAPrivateKey) keyFactory.generatePrivate(rsaPrivateKeySpec);
		} catch (Exception ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	public static byte[] JWK_to_RSAPrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.RSA.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());

		return JWK_to_RSAPrivateKey(jsonWebKey).getEncoded();
	}

	public static ECKey JWK_to_secp256k1PrivateKey(JWK jsonWebKey) {

		if (! KeyType.EC.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.secp256k1.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return ECKey.fromPrivate(jsonWebKey.getDdecoded());
	}

	public static byte[] JWK_to_secp256k1PrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.EC.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.secp256k1.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return jsonWebKey.getDdecoded();
	}

	/*public static KeyPair JWK_to_Bls12381G1PrivateKey(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.Bls12381G1.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return new KeyPair(jsonWebKey.getXdecoded(), jsonWebKey.getDdecoded());
	}

	public static byte[] JWK_to_Bls12381G1PrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.Bls12381G1.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return jsonWebKey.getDdecoded();
	}

	public static KeyPair JWK_to_Bls12381G2PrivateKey(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.Bls12381G2.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return new KeyPair(jsonWebKey.getXdecoded(), jsonWebKey.getDdecoded());
	}

	public static byte[] JWK_to_Bls12381G2PrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.Bls12381G2.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return jsonWebKey.getDdecoded();
	}

	public static KeyPair JWK_to_Bls48581G1PrivateKey(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.Bls48581G1.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return new KeyPair(jsonWebKey.getXdecoded(), jsonWebKey.getDdecoded());
	}

	public static byte[] JWK_to_Bls48581G1PrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.Bls48581G1.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return jsonWebKey.getDdecoded();
	}

	public static KeyPair JWK_to_Bls48581G2PrivateKey(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.Bls48581G2.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return new KeyPair(jsonWebKey.getXdecoded(), jsonWebKey.getDdecoded());
	}

	public static byte[] JWK_to_Bls48581G2PrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.Bls48581G2.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return jsonWebKey.getDdecoded();
	}

	public static byte[] JWK_to_Ed25519PrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.Ed25519.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		byte[] privateKeyBytes = new byte[64];
		System.arraycopy(jsonWebKey.getDdecoded(), 0, privateKeyBytes, 0, 32);
		System.arraycopy(jsonWebKey.getXdecoded(), 0, privateKeyBytes, 32, 32);

		return privateKeyBytes;
	}

	public static byte[] JWK_to_X25519PrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.OKP.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.X25519.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		byte[] privateKeyBytes = new byte[64];
		System.arraycopy(jsonWebKey.getDdecoded(), 0, privateKeyBytes, 0, 32);
		System.arraycopy(jsonWebKey.getXdecoded(), 0, privateKeyBytes, 32, 32);

		return privateKeyBytes;
	}*/

	public static byte[] JWK_to_P_256PrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.EC.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.P_256.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return jsonWebKey.getDdecoded();
	}

	public static byte[] JWK_to_P_384PrivateKeyBytes(JWK jsonWebKey) {

		if (! KeyType.EC.equals(jsonWebKey.getKty())) throw new IllegalArgumentException("Incorrect key type: " + jsonWebKey.getKty());
		if (! Curve.P_384.equals(jsonWebKey.getCrv())) throw new IllegalArgumentException("Incorrect curve: " + jsonWebKey.getCrv());

		return jsonWebKey.getDdecoded();
	}
}
