package info.weboftrust.ldsignatures.signer;

import info.weboftrust.ldsignatures.suites.SignatureSuite;
import info.weboftrust.ldsignatures.suites.SignatureSuites;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LdSignerRegistry {

    /*public static final List<Class<? extends LdSigner<? extends SignatureSuite>>> LD_SIGNERS = List.of(
            RsaSignature2018LdSigner.class,
            Ed25519Signature2018LdSigner.class,
            Ed25519Signature2020LdSigner.class,
            JcsEd25519Signature2020LdSigner.class,
            EcdsaSecp256k1Signature2019LdSigner.class,
            EcdsaKoblitzSignature2016LdSigner.class,
            JcsEcdsaSecp256k1Signature2019LdSigner.class,
            BbsBlsSignature2020LdSigner.class,
            JsonWebSignature2020LdSigner.class
    );*/

    private static final Map<String, Class<? extends LdSigner<? extends SignatureSuite>>> LD_SIGNERS_BY_SIGNATURE_SUITE_TERM = new HashMap<>();

    static {
//        LD_SIGNERS_BY_SIGNATURE_SUITE_TERM = new HashMap<>();

//        for (Class<? extends LdSigner<? extends SignatureSuite>> ldSignerClass : LD_SIGNERS) {
        Class<? extends LdSigner<? extends SignatureSuite>> ldSignerClass = EcdsaSecp256k1Signature2019LdSigner.class;
        Class<? extends SignatureSuite> signatureSuiteClass = (Class<? extends SignatureSuite>) ((ParameterizedType) ldSignerClass.getGenericSuperclass()).getActualTypeArguments()[0];
        String term = SignatureSuites.findSignatureSuiteByClass(signatureSuiteClass).getTerm();
        LD_SIGNERS_BY_SIGNATURE_SUITE_TERM.put(term, ldSignerClass);
//        }
    }

    public static LdSigner<? extends SignatureSuite> getLdSignerBySignatureSuiteTerm(String signatureSuiteTerm) {
        Class<? extends LdSigner<? extends SignatureSuite>> ldSignerClass = LD_SIGNERS_BY_SIGNATURE_SUITE_TERM.get(signatureSuiteTerm);
        if (ldSignerClass == null) throw new IllegalArgumentException();
        LdSigner<? extends SignatureSuite> ldSigner;
        try {
            ldSigner = ldSignerClass.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
        return ldSigner;
    }

    public static LdSigner<? extends SignatureSuite> getLdSignerBySignatureSuite(SignatureSuite signatureSuite) {
        return getLdSignerBySignatureSuiteTerm(signatureSuite.getTerm());
    }
}
