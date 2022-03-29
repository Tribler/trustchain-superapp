package foundation.identity.jsonld.validation;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.http.media.MediaType;
import com.apicatalog.jsonld.processor.ExpansionProcessor;
import foundation.identity.jsonld.JsonLDObject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Map;

public class Validation {

    private static void validateTrue(boolean valid) throws IllegalStateException {

        if (! valid) throw new IllegalStateException();
    }

    private static final String UNDEFINED_TERM_URI = "urn:UNDEFINEDTERM";

    private static void findUndefinedTerms(JsonArray jsonArray) {

        for (JsonValue entry : jsonArray) {

            if (entry instanceof JsonObject) findUndefinedTerms((JsonObject) entry);
        }
    }

    private static void findUndefinedTerms(JsonObject jsonObject) {

        for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {

            if (entry.getKey().startsWith(UNDEFINED_TERM_URI)) {

                throw new RuntimeException("Undefined JSON-LD term: " + entry.getKey().substring(UNDEFINED_TERM_URI.length()));
            }

            if (entry.getValue() instanceof JsonArray) findUndefinedTerms((JsonArray) entry.getValue());
            if (entry.getValue() instanceof JsonObject) findUndefinedTerms((JsonObject) entry.getValue());
        }
    }

    private static void validateJsonLd(JsonLDObject jsonLdObject) {

        try {

            JsonObject expandContext = Json.createObjectBuilder().add("@vocab", Json.createValue(UNDEFINED_TERM_URI)).build();

            JsonDocument jsonDocument = JsonDocument.of(MediaType.JSON_LD, jsonLdObject.toJsonObject());

            JsonLdOptions jsonLdOptions = new JsonLdOptions();
            jsonLdOptions.setDocumentLoader(jsonLdObject.getDocumentLoader());
            jsonLdOptions.setExpandContext(expandContext);

            JsonArray jsonArray = ExpansionProcessor.expand(jsonDocument, jsonLdOptions, false);
            JsonObject jsonObject = jsonArray.getJsonObject(0);

            findUndefinedTerms(jsonObject);
        } catch (JsonLdError ex) {

            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private static void validateRun(Runnable runnable, String message) throws IllegalStateException {

        try {

            runnable.run();
        } catch (Exception ex) {

            if (ex.getMessage() != null && ! ex.getMessage().isEmpty()) message = message + " (" + ex.getMessage().trim() + ")";
            throw new IllegalStateException(message, ex);
        }
    }

    public static void validate(JsonLDObject jsonLdObject) throws IllegalStateException {

        validateRun(() -> { validateJsonLd(jsonLdObject); }, "JSON-LD problem.");
    }
}
