package info.weboftrust.ldsignatures.verifier;

import info.weboftrust.ldsignatures.suites.SignatureSuite;
import info.weboftrust.ldsignatures.suites.SignatureSuites;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LdVerifierRegistry {

    /*public static final List<Class<? extends LdVerifier<? extends SignatureSuite>>> LD_VERIFIERS = List.of(
            RsaSignature2018LdVerifier.class,
            Ed25519Signature2018LdVerifier.class,
            Ed25519Signature2020LdVerifier.class,
            JcsEd25519Signature2020LdVerifier.class,
            EcdsaSecp256k1Signature2019LdVerifier.class,
            EcdsaKoblitzSignature2016LdVerifier.class,
            JcsEcdsaSecp256k1Signature2019LdVerifier.class,
            BbsBlsSignature2020LdVerifier.class,
            JsonWebSignature2020LdVerifier.class
    );*/

    private static final Map<String, Class<? extends LdVerifier<? extends SignatureSuite>>> LD_VERIFIERS_BY_SIGNATURE_SUITE_TERM;

    static {
        LD_VERIFIERS_BY_SIGNATURE_SUITE_TERM = new HashMap<>();
//        for (Class<? extends LdVerifier<? extends SignatureSuite>> ldVerifierClass : LD_VERIFIERS) {
        Class<? extends LdVerifier<? extends SignatureSuite>> ldVerifierClass = EcdsaSecp256k1Signature2019LdVerifier.class;
        Class<? extends SignatureSuite> signatureSuiteClass = (Class<? extends SignatureSuite>) ((ParameterizedType) ldVerifierClass.getGenericSuperclass()).getActualTypeArguments()[0];
        String term = SignatureSuites.findSignatureSuiteByClass(signatureSuiteClass).getTerm();
        LD_VERIFIERS_BY_SIGNATURE_SUITE_TERM.put(term, ldVerifierClass);
//        }
    }

    public static LdVerifier<? extends SignatureSuite> getLdVerifierBySignatureSuiteTerm(String signatureSuiteTerm) {
        Class<? extends LdVerifier<? extends SignatureSuite>> ldVerifierClass = LD_VERIFIERS_BY_SIGNATURE_SUITE_TERM.get(signatureSuiteTerm);
        if (ldVerifierClass == null) throw new IllegalArgumentException();
        LdVerifier<? extends SignatureSuite> ldVerifier;
        try {
            ldVerifier = ldVerifierClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        return ldVerifier;
    }

    public static LdVerifier<? extends SignatureSuite> getLdVerifierBySignatureSuite(SignatureSuite signatureSuite) {
        return getLdVerifierBySignatureSuiteTerm(signatureSuite.getTerm());
    }
}
