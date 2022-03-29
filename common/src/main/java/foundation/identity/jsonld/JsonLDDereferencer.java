package foundation.identity.jsonld;

import com.apicatalog.jsonld.uri.UriResolver;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class JsonLDDereferencer {

    public static class Function implements java.util.function.Function<Object, JsonLDObject> {

        private JsonLDObject jsonLdDocument;
        private URI baseUri;
        private boolean ignoreDereferencingErrors;
        private Predicate<JsonLDObject> predicate;

        public Function(JsonLDObject jsonLdDocument, URI baseUri, boolean ignoreDereferencingErrors, Predicate<JsonLDObject> predicate) {
            this.jsonLdDocument = jsonLdDocument;
            this.baseUri = baseUri;
            this.ignoreDereferencingErrors = ignoreDereferencingErrors;
            this.predicate = predicate;
        }

        public Function(JsonLDObject jsonLdDocument, URI baseUri, boolean ignoreDereferencingErrors) {
            this.jsonLdDocument = jsonLdDocument;
            this.baseUri = baseUri;
            this.ignoreDereferencingErrors = ignoreDereferencingErrors;
            this.predicate = null;
        }

        public Function(JsonLDObject jsonLdDocument, URI baseUri) {
            this.jsonLdDocument = jsonLdDocument;
            this.baseUri = baseUri;
            this.ignoreDereferencingErrors = false;
            this.predicate = null;
        }

        public Function(JsonLDObject jsonLdDocument) {
            this.jsonLdDocument = jsonLdDocument;
            this.baseUri = null;
            this.ignoreDereferencingErrors = false;
            this.predicate = null;
        }

        @Override
        public JsonLDObject apply(Object o) {

            URI uri = null;
            JsonLDObject result = null;

            if (o instanceof JsonLDObject) {
                result = (JsonLDObject) o;
            } else if (o instanceof Map) {
                result = JsonLDObject.fromMap((Map<String, Object>) o);
            } else if (o instanceof String) {
                try {
                    uri = new URI((String) o);
                } catch (URISyntaxException ex) {
                    if (this.ignoreDereferencingErrors) return null; else throw new IllegalArgumentException("Cannot dereference non-URI string: " + o);
                }
                result = findByIdInJsonLdObject(this.jsonLdDocument, uri, this.baseUri);
            } else {
                if (this.ignoreDereferencingErrors) return null; else throw new IllegalArgumentException("Cannot dereference non-URI value: " + o);
            }

            if (result != null && this.predicate != null) {
                boolean test = this.predicate.test(result);
                if (!test) {
                    if (this.ignoreDereferencingErrors) return null; else throw new IllegalArgumentException("Unacceptable result for dereferencing URI " + uri);
                }
            }

            if (result == null) {
                if (this.ignoreDereferencingErrors) return null; else throw new IllegalArgumentException("No result for dereferencing URI " + uri);
            }

            return result;
        }
    }

    public static JsonLDObject findByIdInJsonLdObject(JsonLDObject jsonLdObject, URI uri, URI baseUri) {

        if (jsonLdObject.getId() != null) {

            URI findId = uri;
            if (! findId.isAbsolute() && baseUri == null) throw new IllegalArgumentException("No base URI for relative uri " + findId);
            findId = URI.create(UriResolver.resolve(baseUri, findId.toString()));

            URI idUri = jsonLdObject.getId();
            if (! idUri.isAbsolute() && baseUri == null) throw new IllegalArgumentException("No base URI for relative 'id' uri " + uri);
            idUri = URI.create(UriResolver.resolve(baseUri, idUri.toString()));

            if (findId.equals(idUri)) return jsonLdObject;
        }

        for (Object value : jsonLdObject.getJsonObject().values()) {
            if (value instanceof Map) {
                JsonLDObject foundJsonLDObject = findByIdInJsonLdObject(JsonLDObject.fromMap((Map<String, Object>) value), uri, baseUri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
            else if (value instanceof List) {
                JsonLDObject foundJsonLDObject = findByIdInList((List<Object>) value, uri, baseUri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
        }

        return null;
    }

    private static JsonLDObject findByIdInList(List<Object> list, URI uri, URI baseUri) {

        for (Object value : list) {
            if (value instanceof Map) {
                JsonLDObject foundJsonLDObject = findByIdInJsonLdObject(JsonLDObject.fromMap((Map<String, Object>) value), uri, baseUri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
            else if (value instanceof List) {
                JsonLDObject foundJsonLDObject = findByIdInList((List<Object>) value, uri, baseUri);
                if (foundJsonLDObject != null) return foundJsonLDObject;
            }
        }

        return null;
    }
}
