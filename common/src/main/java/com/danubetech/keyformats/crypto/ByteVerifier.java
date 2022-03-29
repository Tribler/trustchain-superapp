package com.danubetech.keyformats.crypto;

import java.security.GeneralSecurityException;

public abstract class ByteVerifier {

	private final String algorithm;

	protected ByteVerifier(String algorithm) {

		this.algorithm = algorithm;
	}

	public final boolean verify(byte[] content, byte[] signature, String algorithm) throws GeneralSecurityException {

		if (! algorithm.equals(this.algorithm)) throw new GeneralSecurityException("Unexpected algorithm " + algorithm + " is different from " + this.algorithm);

		return this.verify(content, signature);
	}

	protected abstract boolean verify(byte[] content, byte[] signature) throws GeneralSecurityException;

	public String getAlgorithm() {

		return this.algorithm;
	}
}
