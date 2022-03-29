package com.danubetech.keyformats.crypto;

import java.security.GeneralSecurityException;

public abstract class ByteSigner {

	private final String algorithm;

	protected ByteSigner(String algorithm) {

		this.algorithm = algorithm;
	}

	public final byte[] sign(byte[] content, String algorithm) throws GeneralSecurityException {

		if (! algorithm.equals(this.algorithm)) throw new GeneralSecurityException("Unexpected algorithm " + algorithm + " is different from " + this.algorithm);

		return this.sign(content);
	}

	protected abstract byte[] sign(byte[] content) throws GeneralSecurityException;

	public String getAlgorithm() {

		return this.algorithm;
	}
}
