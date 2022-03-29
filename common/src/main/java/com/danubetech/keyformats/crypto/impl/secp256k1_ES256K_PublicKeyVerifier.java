package com.danubetech.keyformats.crypto.impl;

import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.jose.JWSAlgorithm;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;

import java.math.BigInteger;
import java.security.GeneralSecurityException;

public class secp256k1_ES256K_PublicKeyVerifier extends PublicKeyVerifier<ECKey> {

    public secp256k1_ES256K_PublicKeyVerifier(ECKey publicKey) {

        super(publicKey, JWSAlgorithm.ES256K);
    }

    @Override
    public boolean verify(byte[] content, byte[] signature) throws GeneralSecurityException {

        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(signature, 0, r, 0, r.length);
        System.arraycopy(signature, 32, s, 0, s.length);

        ECKey.ECDSASignature ecdsaSignature = new ECKey.ECDSASignature(new BigInteger(1, r), new BigInteger(1, s));

        return this.getPublicKey().verify(Sha256Hash.of(content), ecdsaSignature);
    }
}
