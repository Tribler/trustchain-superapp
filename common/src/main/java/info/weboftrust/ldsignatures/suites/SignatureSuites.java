package info.weboftrust.ldsignatures.suites;

import com.danubetech.keyformats.jose.KeyTypeName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignatureSuites {

	public static final RsaSignature2018SignatureSuite SIGNATURE_SUITE_RSASIGNATURE2018 = new RsaSignature2018SignatureSuite();
	public static final Ed25519Signature2018SignatureSuite SIGNATURE_SUITE_ED25519SIGNATURE2018 = new Ed25519Signature2018SignatureSuite();
	public static final Ed25519Signature2020SignatureSuite SIGNATURE_SUITE_ED25519SIGNATURE2020 = new Ed25519Signature2020SignatureSuite();
	public static final JcsEd25519Signature2020SignatureSuite SIGNATURE_SUITE_JCSED25519SIGNATURE2020 = new JcsEd25519Signature2020SignatureSuite();
	public static final EcdsaSecp256k1Signature2019SignatureSuite SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019 = new EcdsaSecp256k1Signature2019SignatureSuite();
	public static final EcdsaKoblitzSignature2016SignatureSuite SIGNATURE_SUITE_ECDSAKOBLITZSIGNATURE2016 = new EcdsaKoblitzSignature2016SignatureSuite();
	public static final JcsEcdsaSecp256k1Signature2019SignatureSuite SIGNATURE_SUITE_JCSECDSASECP256L1SIGNATURE2019 = new JcsEcdsaSecp256k1Signature2019SignatureSuite();
	public static final BbsBlsSignature2020SignatureSuite SIGNATURE_SUITE_BBSBLSSIGNATURE2020 = new BbsBlsSignature2020SignatureSuite();
	public static final JsonWebSignature2020SignatureSuite SIGNATURE_SUITE_JSONWEBSIGNATURE2020 = new JsonWebSignature2020SignatureSuite();

	public static List<? extends SignatureSuite> SIGNATURE_SUITES = List.of(
			SIGNATURE_SUITE_RSASIGNATURE2018,
			SIGNATURE_SUITE_ED25519SIGNATURE2018,
			SIGNATURE_SUITE_ED25519SIGNATURE2020,
			SIGNATURE_SUITE_JCSED25519SIGNATURE2020,
			SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019,
			SIGNATURE_SUITE_ECDSAKOBLITZSIGNATURE2016,
			SIGNATURE_SUITE_JCSECDSASECP256L1SIGNATURE2019,
			SIGNATURE_SUITE_BBSBLSSIGNATURE2020,
			SIGNATURE_SUITE_JSONWEBSIGNATURE2020
	);

	private static final Map<Class<? extends SignatureSuite>, SignatureSuite> SIGNATURE_SUITES_BY_SIGNATURE_SUITE_CLASS;
	private static final Map<String, SignatureSuite> SIGNATURE_SUITES_BY_TERM;
	private static final Map<KeyTypeName, List<SignatureSuite>> SIGNATURE_SUITES_BY_KEY_TYPE_NAME;

	static {
		SIGNATURE_SUITES_BY_SIGNATURE_SUITE_CLASS = new HashMap<>();
		for (SignatureSuite signatureSuite : SIGNATURE_SUITES) {
			Class<? extends SignatureSuite> signatureSuiteClass = signatureSuite.getClass();
			SIGNATURE_SUITES_BY_SIGNATURE_SUITE_CLASS.put(signatureSuiteClass, signatureSuite);
		}
	}

	static {
		SIGNATURE_SUITES_BY_TERM = new HashMap<>();
		for (SignatureSuite signatureSuite : SIGNATURE_SUITES) {
			String signatureSuiteTerm = signatureSuite.getTerm();
			SIGNATURE_SUITES_BY_TERM.put(signatureSuiteTerm, signatureSuite);
		}
	}

	static {
		SIGNATURE_SUITES_BY_KEY_TYPE_NAME = new HashMap<>();
		for (SignatureSuite signatureSuite : SIGNATURE_SUITES) {
			List<KeyTypeName> keyTypeNames = signatureSuite.getKeyTypeNames();
			for (KeyTypeName keyTypeName : keyTypeNames) {
				List<SignatureSuite> signatureSuitesList = SIGNATURE_SUITES_BY_KEY_TYPE_NAME.get(keyTypeName);
				if (signatureSuitesList == null) {
					signatureSuitesList = new ArrayList<>();
					SIGNATURE_SUITES_BY_KEY_TYPE_NAME.put(keyTypeName, signatureSuitesList);
				}
				signatureSuitesList.add(signatureSuite);
			}
		}
	}

	public static SignatureSuite findSignatureSuiteByClass(Class<? extends SignatureSuite> clazz) {
		return SIGNATURE_SUITES_BY_SIGNATURE_SUITE_CLASS.get(clazz);
	}

	public static SignatureSuite findSignatureSuiteByTerm(String signatureSuiteTerm) {
		return SIGNATURE_SUITES_BY_TERM.get(signatureSuiteTerm);
	}

	public static List<SignatureSuite> findSignatureSuitesByKeyTypeName(KeyTypeName keyTypeName) {
		return SIGNATURE_SUITES_BY_KEY_TYPE_NAME.get(keyTypeName);
	}

	public static SignatureSuite findDefaultSignatureSuiteByKeyTypeName(KeyTypeName keyTypeName) {
		List<SignatureSuite> foundSignatureSuitesByKeyTypeName = findSignatureSuitesByKeyTypeName(keyTypeName);
		return foundSignatureSuitesByKeyTypeName == null ? null : foundSignatureSuitesByKeyTypeName.get(0);
	}
}
