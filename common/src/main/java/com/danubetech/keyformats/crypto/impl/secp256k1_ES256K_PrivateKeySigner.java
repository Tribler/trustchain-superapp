package com.danubetech.keyformats.crypto.impl;

import com.danubetech.keyformats.crypto.PrivateKeySigner;
import com.danubetech.keyformats.jose.JWSAlgorithm;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;

import java.security.GeneralSecurityException;

public class secp256k1_ES256K_PrivateKeySigner extends PrivateKeySigner<ECKey> {

    public secp256k1_ES256K_PrivateKeySigner(ECKey privateKey) {

        super(privateKey, JWSAlgorithm.ES256K);
    }

    @Override
    public byte[] sign(byte[] content) throws GeneralSecurityException {

        ECKey.ECDSASignature ecdsaSignature = this.getPrivateKey().sign(Sha256Hash.of(content));
        byte[] r = Utils.bigIntegerToBytes(ecdsaSignature.r, 32);
        byte[] s = Utils.bigIntegerToBytes(ecdsaSignature.s, 32);

        byte[] signatureBytes = new byte[64];
        System.arraycopy(r, 0, signatureBytes, 0, r.length);
        System.arraycopy(s, 0, signatureBytes, 32, s.length);
        return signatureBytes;
    }
}
