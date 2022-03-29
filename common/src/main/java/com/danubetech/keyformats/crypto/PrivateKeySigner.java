package com.danubetech.keyformats.crypto;

public abstract class PrivateKeySigner <T> extends ByteSigner {

	private T privateKey;

	protected PrivateKeySigner(T privateKey, String algorithm) {

		super(algorithm);

		this.privateKey = privateKey;
	}

	protected T getPrivateKey() {

		return this.privateKey;
	}
}
