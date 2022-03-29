package info.weboftrust.ldsignatures.suites;

import com.danubetech.keyformats.jose.JWSAlgorithm;
import com.danubetech.keyformats.jose.KeyTypeName;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class JcsEcdsaSecp256k1Signature2019SignatureSuite extends SignatureSuite {

	JcsEcdsaSecp256k1Signature2019SignatureSuite() {

		super(
				"JcsEcdsaSecp256k1Signature2019",
				URI.create("https://w3id.org/security#JcsEcdsaSecp256k1Signature2019"),
				URI.create("https://tools.ietf.org/html/draft-rundgren-json-canonicalization-scheme-16"),
				URI.create("http://w3id.org/digests#sha256"),
				URI.create("http://w3id.org/security#secp256k1"),
				List.of(KeyTypeName.secp256k1),
				Map.of(KeyTypeName.secp256k1, List.of(JWSAlgorithm.ES256K)));
	}
}
