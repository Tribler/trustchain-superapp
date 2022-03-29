package info.weboftrust.ldsignatures.verifier;

import com.danubetech.keyformats.crypto.ByteVerifier;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.canonicalizer.Canonicalizer;
import info.weboftrust.ldsignatures.suites.SignatureSuite;
import info.weboftrust.ldsignatures.util.SHAUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;

public abstract class LdVerifier<SIGNATURESUITE extends SignatureSuite> {

    private final SIGNATURESUITE signatureSuite;

    private ByteVerifier verifier;
    private Canonicalizer canonicalizer;

    protected LdVerifier(SIGNATURESUITE signatureSuite, ByteVerifier verifier, Canonicalizer canonicalizer) {

        this.signatureSuite = signatureSuite;
        this.verifier = verifier;
        this.canonicalizer = canonicalizer;
    }

    /**
     * @deprecated
     * Use LdVerifierRegistry.getLdVerifierBySignatureSuiteTerm(signatureSuiteTerm) instead.
     */
    @Deprecated
    public static LdVerifier<? extends SignatureSuite> ldVerifierForSignatureSuite(String signatureSuiteTerm) {
        return LdVerifierRegistry.getLdVerifierBySignatureSuiteTerm(signatureSuiteTerm);
    }

    /**
     * @deprecated
     * Use LdVerifierRegistry.getLdVerifierBySignatureSuite(signatureSuite) instead.
     */
    @Deprecated
    public static LdVerifier<? extends SignatureSuite> ldVerifierForSignatureSuite(SignatureSuite signatureSuite) {
        return LdVerifierRegistry.getLdVerifierBySignatureSuite(signatureSuite);
    }

    public abstract boolean verify(byte[] signingInput, LdProof ldProof) throws GeneralSecurityException;

    public boolean verify(JsonLDObject jsonLdObject, LdProof ldProof) throws IOException, GeneralSecurityException, JsonLDException {

        // check the proof object

        if (!this.getSignatureSuite().getTerm().equals(ldProof.getType()))
            throw new GeneralSecurityException("Unexpected signature type: " + ldProof.getType() + " is not " + this.getSignatureSuite().getTerm());

        // obtain the canonicalized document

        byte[] canonicalizationResult = this.getCanonicalizer().canonicalize(ldProof, jsonLdObject);

        // verify

        boolean verify = this.verify(canonicalizationResult, ldProof);

        // done

        return verify;
    }

    public boolean verify(JsonLDObject jsonLdObject) throws IOException, GeneralSecurityException, JsonLDException {

        // obtain the signature object

        LdProof ldProof = LdProof.getFromJsonLDObject(jsonLdObject);
        if (ldProof == null) return false;

        // done

        return this.verify(jsonLdObject, ldProof);
    }

    public SignatureSuite getSignatureSuite() {
        return this.signatureSuite;
    }

    /*
     * Getters and setters
     */

    public ByteVerifier getVerifier() {
        return this.verifier;
    }

    public void setVerifier(ByteVerifier verifier) {
        this.verifier = verifier;
    }

    public Canonicalizer getCanonicalizer() {
        return canonicalizer;
    }

    public void setCanonicalizer(Canonicalizer canonicalizer) {
        this.canonicalizer = canonicalizer;
    }
}
