package info.weboftrust.ldsignatures;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.fasterxml.jackson.annotation.JsonCreator;
import foundation.identity.jsonld.JsonLDObject;
import foundation.identity.jsonld.JsonLDUtils;
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts;
import info.weboftrust.ldsignatures.jsonld.LDSecurityKeywords;

import java.io.Reader;
import java.net.URI;
import java.util.Date;
import java.util.Map;

public class LdProof extends JsonLDObject {

//	public static final URI[] DEFAULT_JSONLD_CONTEXTS = { LDSecurityContexts.JSONLD_CONTEXT_W3ID_SECURITY_V3 };
//	public static final String[] DEFAULT_JSONLD_TYPES = { };
//	public static final String DEFAULT_JSONLD_PREDICATE = LDSecurityKeywords.JSONLD_TERM_PROOF;
//	public static final DocumentLoader DEFAULT_DOCUMENT_LOADER = LDSecurityContexts.DOCUMENT_LOADER;

	@JsonCreator
	public LdProof() {
		super();
	}

	protected LdProof(Map<String, Object> jsonObject) {
		super(jsonObject);
	}

	/*
	 * Factory methods
	 */

	public static class Builder<B extends Builder<B>> extends JsonLDObject.Builder<B> {

		private URI creator;
		private Date created;
		private String domain;
		private String challenge;
		private String nonce;
		private String proofPurpose;
		private URI verificationMethod;
		private String proofValue;
		private String jws;

		public Builder(LdProof jsonLdObject) {
			super(jsonLdObject);
		}

		@Override
		public LdProof build() {

			super.build();

			// add JSON-LD properties
			if (this.creator != null) JsonLDUtils.jsonLdAdd(this.jsonLdObject, LDSecurityKeywords.JSONLD_TERM_CREATOR, JsonLDUtils.uriToString(this.creator));
			if (this.created != null) JsonLDUtils.jsonLdAdd(this.jsonLdObject, LDSecurityKeywords.JSONLD_TERM_CREATED, JsonLDUtils.dateToString(this.created));
			if (this.domain != null) JsonLDUtils.jsonLdAdd(this.jsonLdObject, LDSecurityKeywords.JSONLD_TERM_DOMAIN, this.domain);
			if (this.challenge != null) JsonLDUtils.jsonLdAdd(this.jsonLdObject, LDSecurityKeywords.JSONLD_TERM_CHALLENGE, this.challenge);
			if (this.nonce != null) JsonLDUtils.jsonLdAdd(this.jsonLdObject, LDSecurityKeywords.JSONLD_TERM_NONCE, this.nonce);
			if (this.proofPurpose != null) JsonLDUtils.jsonLdAdd(this.jsonLdObject, LDSecurityKeywords.JSONLD_TERM_PROOFPURPOSE, this.proofPurpose);
			if (this.verificationMethod != null) JsonLDUtils.jsonLdAdd(this.jsonLdObject, LDSecurityKeywords.JSONLD_TERM_VERIFICATIONMETHOD, JsonLDUtils.uriToString(this.verificationMethod));
			if (this.proofValue != null) JsonLDUtils.jsonLdAdd(this.jsonLdObject, LDSecurityKeywords.JSONLD_TERM_PROOFVALUE, this.proofValue);
			if (this.jws != null) JsonLDUtils.jsonLdAdd(this.jsonLdObject, LDSecurityKeywords.JSONLD_TERM_JWS, this.jws);

			return (LdProof) this.jsonLdObject;
		}

		public B creator(URI creator) {
			this.creator = creator;
			return (B) this;
		}

		public B created(Date created) {
			this.created = created;
			return (B) this;
		}

		public B domain(String domain) {
			this.domain = domain;
			return (B) this;
		}

		public B challenge(String challenge) {
			this.challenge = challenge;
			return (B) this;
		}

		public B nonce(String nonce) {
			this.nonce = nonce;
			return (B) this;
		}

		public B proofPurpose(String proofPurpose) {
			this.proofPurpose = proofPurpose;
			return (B) this;
		}

		public B verificationMethod(URI verificationMethod) {
			this.verificationMethod = verificationMethod;
			return (B) this;
		}

		public B proofValue(String proofValue) {
			this.proofValue = proofValue;
			return (B) this;
		}

		public B jws(String jws) {
			this.jws = jws;
			return (B) this;
		}
	}

	public static Builder<? extends Builder<?>> builder() {
		return new Builder(new LdProof());
	}

	public static LdProof fromJsonObject(Map<String, Object> jsonObject) {
		return new LdProof(jsonObject);
	}

	public static LdProof fromJsonLDObject(JsonLDObject jsonLDObject) { return fromJsonObject(jsonLDObject.getJsonObject()); }

	public static LdProof fromJson(Reader reader) {
		return new LdProof(readJson(reader));
	}

	public static LdProof fromJson(String json) {
		return new LdProof(readJson(json));
	}

	public static LdProof fromMap(Map<String, Object> jsonObject) {
		return new LdProof(jsonObject);
	}

	/*
	 * Adding, getting, and removing the JSON-LD object
	 */

	public static LdProof getFromJsonLDObject(JsonLDObject jsonLdObject) {
		return JsonLDObject.getFromJsonLDObject(LdProof.class, jsonLdObject);
	}

	public static void removeFromJsonLdObject(JsonLDObject jsonLdObject) {
		JsonLDObject.removeFromJsonLdObject(LdProof.class, jsonLdObject);
	}

	/*
	 * Helper methods
	 */

	public static void removeLdProofValues(JsonLDObject jsonLdObject) {
		JsonLDUtils.jsonLdRemove(jsonLdObject, LDSecurityKeywords.JSONLD_TERM_PROOFVALUE);
		JsonLDUtils.jsonLdRemove(jsonLdObject, LDSecurityKeywords.JSONLD_TERM_JWS);
		JsonLDUtils.jsonLdRemove(jsonLdObject, "signatureValue");
	}

	/*
	 * Getters
	 */

	public URI getCreator() {
		return JsonLDUtils.stringToUri(JsonLDUtils.jsonLdGetString(this.getJsonObject(), LDSecurityKeywords.JSONLD_TERM_CREATOR));
	}

	public Date getCreated() {
		return JsonLDUtils.stringToDate(JsonLDUtils.jsonLdGetString(this.getJsonObject(), LDSecurityKeywords.JSONLD_TERM_CREATED));
	}

	public String getDomain() {
		return JsonLDUtils.jsonLdGetString(this.getJsonObject(), LDSecurityKeywords.JSONLD_TERM_DOMAIN);
	}

	public String getChallenge() {
		return JsonLDUtils.jsonLdGetString(this.getJsonObject(), LDSecurityKeywords.JSONLD_TERM_CHALLENGE);
	}

	public String getNonce() {
		return JsonLDUtils.jsonLdGetString(this.getJsonObject(), LDSecurityKeywords.JSONLD_TERM_NONCE);
	}

	public String getProofPurpose() {
		return JsonLDUtils.jsonLdGetString(this.getJsonObject(), LDSecurityKeywords.JSONLD_TERM_PROOFPURPOSE);
	}

	public URI getVerificationMethod() {
		return JsonLDUtils.stringToUri(JsonLDUtils.jsonLdGetString(this.getJsonObject(), LDSecurityKeywords.JSONLD_TERM_VERIFICATIONMETHOD));
	}

	public String getProofValue() {
		return JsonLDUtils.jsonLdGetString(this.getJsonObject(), LDSecurityKeywords.JSONLD_TERM_PROOFVALUE);
	}

	public String getJws() {
		return JsonLDUtils.jsonLdGetString(this.getJsonObject(), LDSecurityKeywords.JSONLD_TERM_JWS);
	}
}
