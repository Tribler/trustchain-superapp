package com.danubetech.keyformats.crypto.provider;

import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class Ed25519Provider {

	private static Ed25519Provider instance;

	public static Ed25519Provider get() {

		Ed25519Provider result = instance;

		if (result == null) {

			synchronized(Ed25519Provider.class) {

				result = instance;

				if (result == null) {

					ServiceLoader<Ed25519Provider> serviceLoader = ServiceLoader.load(Ed25519Provider.class, Ed25519Provider.class.getClassLoader());
					Iterator<Ed25519Provider> iterator = serviceLoader.iterator();
					if (! iterator.hasNext()) throw new RuntimeException("No " + Ed25519Provider.class.getName() + " registered");

					instance = result = iterator.next();
				}
			}
		}

		return result;
	}

	public static void set(Ed25519Provider instance) {

		Ed25519Provider.instance = instance;
	}

	public abstract void generateEC25519KeyPair(byte[] publicKey, byte[] privateKey) throws GeneralSecurityException;
	public abstract void generateEC25519KeyPairFromSeed(byte[] publicKey, byte[] privateKey, byte[] seed) throws GeneralSecurityException;
	public abstract byte[] sign(byte[] content, byte[] privateKey) throws GeneralSecurityException;
	public abstract boolean verify(byte[] content, byte[] signature, byte[] publicKey) throws GeneralSecurityException;
}
