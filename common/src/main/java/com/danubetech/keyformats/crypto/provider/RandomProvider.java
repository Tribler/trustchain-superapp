package com.danubetech.keyformats.crypto.provider;

import java.security.GeneralSecurityException;
import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class RandomProvider {

	private static RandomProvider instance;

	public static RandomProvider get() {

		RandomProvider result = instance;

		if (result == null) {

			synchronized(RandomProvider.class) {

				result = instance;

				if (result == null) {

					ServiceLoader<RandomProvider> serviceLoader = ServiceLoader.load(RandomProvider.class, RandomProvider.class.getClassLoader());
					Iterator<RandomProvider> iterator = serviceLoader.iterator();
					if (! iterator.hasNext()) throw new RuntimeException("No " + RandomProvider.class.getName() + " registered");

					instance = result = iterator.next();
				}
			}
		}

		return result;
	}

	public static void set(RandomProvider instance) {

		RandomProvider.instance = instance;
	}

	public abstract byte[] randomBytes(int length) throws GeneralSecurityException;
}
