package com.danubetech.keyformats.crypto;

//import bbs.signatures.KeyPair;
import com.danubetech.keyformats.crypto.impl.*;
import com.danubetech.keyformats.jose.JWSAlgorithm;
import com.danubetech.keyformats.jose.KeyTypeName;
import org.bitcoinj.core.ECKey;

import java.security.interfaces.RSAPrivateKey;

public class PrivateKeySignerFactory {

    public static PrivateKeySigner<?> privateKeySignerForKey(KeyTypeName keyTypeName, String algorithm, Object privateKey) {

        if (keyTypeName == null) throw new NullPointerException("No key type name provided.");
        if (algorithm == null) throw new NullPointerException("No algorithm provided.");
        if (privateKey == null) throw new NullPointerException("No private key provided.");

        if (KeyTypeName.RSA.equals(keyTypeName)) {

//            if (JWSAlgorithm.RS256.equals(algorithm)) return new RSA_RS256_PrivateKeySigner((RSAPrivateKey) privateKey);
//            if (JWSAlgorithm.PS256.equals(algorithm)) return new RSA_PS256_PrivateKeySigner((RSAPrivateKey) privateKey);
        } else if (KeyTypeName.secp256k1.equals(keyTypeName)) {

            if (JWSAlgorithm.ES256K.equals(algorithm)) return new secp256k1_ES256K_PrivateKeySigner((ECKey) privateKey);
        } else if (KeyTypeName.Bls12381G1.equals(keyTypeName)) {

//            if (JWSAlgorithm.BBSPlus.equals(algorithm)) return new Bls12381G1_BBSPlus_PrivateKeySigner((KeyPair) privateKey);
        } else if (KeyTypeName.Bls12381G2.equals(keyTypeName)) {

//            if (JWSAlgorithm.BBSPlus.equals(algorithm)) return new Bls12381G2_BBSPlus_PrivateKeySigner((KeyPair) privateKey);
        } else if (KeyTypeName.Ed25519.equals(keyTypeName)) {

//            if (JWSAlgorithm.EdDSA.equals(algorithm)) return new Ed25519_EdDSA_PrivateKeySigner((byte[]) privateKey);
        }

        throw new IllegalArgumentException("Unsupported private key " + keyTypeName + " and/or algorithm " + algorithm);
    }
}
