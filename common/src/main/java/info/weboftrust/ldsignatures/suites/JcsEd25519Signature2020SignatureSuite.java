package info.weboftrust.ldsignatures.suites;

import com.danubetech.keyformats.jose.JWSAlgorithm;
import com.danubetech.keyformats.jose.KeyTypeName;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class JcsEd25519Signature2020SignatureSuite extends SignatureSuite {

	JcsEd25519Signature2020SignatureSuite() {

		super(
				"JcsEd25519Signature2020",
				URI.create("https://w3id.org/security#JcsEd25519Signature2020"),
				URI.create("https://tools.ietf.org/html/draft-rundgren-json-canonicalization-scheme-16"),
				URI.create("http://w3id.org/digests#sha256"),
				URI.create("http://w3id.org/security#ed25519"),
				List.of(KeyTypeName.Ed25519),
				Map.of(KeyTypeName.Ed25519, List.of(JWSAlgorithm.EdDSA)));
	}
}
