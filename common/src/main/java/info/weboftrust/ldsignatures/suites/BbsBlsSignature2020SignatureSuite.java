package info.weboftrust.ldsignatures.suites;

import com.danubetech.keyformats.jose.JWSAlgorithm;
import com.danubetech.keyformats.jose.KeyTypeName;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class BbsBlsSignature2020SignatureSuite extends SignatureSuite {

	BbsBlsSignature2020SignatureSuite() {

		super(
				"BbsBlsSignature2020",
				URI.create("https://w3id.org/security#BbsBlsSignature2020"),
				URI.create("https://w3id.org/security#URDNA2015"),
				URI.create("https://www.blake2.net/"),
				URI.create("https://electriccoin.co/blog/new-snark-curve/"),
				List.of(KeyTypeName.Bls12381G1,
						KeyTypeName.Bls12381G2),
				Map.of(KeyTypeName.Bls12381G1, List.of(JWSAlgorithm.BBSPlus),
						KeyTypeName.Bls12381G2, List.of(JWSAlgorithm.BBSPlus)));
	}
}
