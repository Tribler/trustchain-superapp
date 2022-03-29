package com.danubetech.keyformats.crypto.provider.impl;

import com.danubetech.keyformats.crypto.provider.RandomProvider;

import java.security.GeneralSecurityException;
import java.util.Random;

public class JavaRandomProvider extends RandomProvider {

	private static final Random RANDOM = new Random();

	@Override
	public byte[] randomBytes(int length) throws GeneralSecurityException {

		byte[] randomBytes = new byte[length];
		RANDOM.nextBytes(randomBytes);
		return randomBytes;
	}
}
