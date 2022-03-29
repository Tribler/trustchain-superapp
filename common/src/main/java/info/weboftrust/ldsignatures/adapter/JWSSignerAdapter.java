package info.weboftrust.ldsignatures.adapter;

import com.danubetech.keyformats.crypto.ByteSigner;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.impl.BaseJWSProvider;
import com.nimbusds.jose.util.Base64URL;

import java.security.GeneralSecurityException;
import java.util.Collections;

public class JWSSignerAdapter extends BaseJWSProvider implements JWSSigner {

	private ByteSigner signer;

	public JWSSignerAdapter(ByteSigner signer, JWSAlgorithm algorithm) {

		super(Collections.singleton(algorithm));

		this.signer = signer;
	}

	@Override
	public Base64URL sign(final JWSHeader header, final byte[] signingInput) throws JOSEException {

		if (! this.supportedJWSAlgorithms().contains(header.getAlgorithm())) throw new JOSEException("Unexpected algorithm: " + header.getAlgorithm());

		try {

			byte[] signature = this.signer.sign(signingInput, header.getAlgorithm().getName());
			return Base64URL.encode(signature);
		} catch (GeneralSecurityException ex) {

			throw new JOSEException(ex.getMessage(), ex);
		}
	}

	/*
	 * Getters and setters
	 */

	public ByteSigner getSigner() {

		return this.signer;
	}

	public void setSigner(ByteSigner signer) {

		this.signer = signer;
	}
}