package info.weboftrust.ldsignatures.adapter;

import com.danubetech.keyformats.crypto.ByteVerifier;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.impl.BaseJWSProvider;
import com.nimbusds.jose.util.Base64URL;

import java.security.GeneralSecurityException;
import java.util.Collections;

public class JWSVerifierAdapter extends BaseJWSProvider implements JWSVerifier {

	private ByteVerifier verifier;

	public JWSVerifierAdapter(ByteVerifier verifier, JWSAlgorithm algorithm) {

		super(Collections.singleton(algorithm));

		this.verifier = verifier;
	}

	@Override
	public boolean verify(JWSHeader header, byte[] signingInput, Base64URL signature) throws JOSEException {

		if (! this.supportedJWSAlgorithms().contains(header.getAlgorithm())) throw new JOSEException("Unexpected algorithm: " + header.getAlgorithm());

		try {

			return this.verifier.verify(signingInput, signature.decode(), header.getAlgorithm().getName());
		} catch (GeneralSecurityException ex) {

			throw new JOSEException(ex.getMessage(), ex);
		}
	}
}