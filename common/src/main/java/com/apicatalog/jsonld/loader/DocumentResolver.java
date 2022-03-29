package com.apicatalog.jsonld.loader;

import java.io.InputStream;
import java.util.logging.Logger;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.apicatalog.jsonld.http.media.MediaType;

class DocumentResolver {

    private static final Logger LOGGER = Logger.getLogger(DocumentResolver.class.getName());

    private MediaType fallbackContentType;

    public DocumentResolver() {
        this.fallbackContentType = null;
    }

    /**
     * Return a reader or throw {@link JsonLdError} if there is no reader nor fallbackContentType.
     *
     * @param contentType content type of the requested reader
     * @return a reader allowing to transform an input into {@link Document}
     * @throws JsonLdError
     */
    public DocumentReader<String> getReader(MediaType contentType) throws JsonLdError {
        DocumentReader<String> reader = findReader(contentType);

        if (reader != null) {
            return reader;
        }  else if (fallbackContentType != null) {
            return findReader(fallbackContentType);
        } else {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED,
                "Unsupported media type '" + contentType
                    + "'. Supported content types are ["
                    + MediaType.JSON_LD + ", "
                    + MediaType.JSON  + ", +json, "
                    //+ (Rdf.canRead().stream().map(MediaType::toString).collect(Collectors.joining(", ")))
                    + "]"
            );
        }

        /*return findReader(contentType)
            .or(() -> {

                if (fallbackContentType != null) {
                    LOGGER.log(Level.WARNING, "Content type [{0}] is not acceptable, trying again with [{1}].", new Object[] { contentType, fallbackContentType});
                    return findReader(fallbackContentType);
                }

                return Optional.empty();
            })
            .orElseThrow(() -> new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED,
                "Unsupported media type '" + contentType
                    + "'. Supported content types are ["
                    + MediaType.JSON_LD + ", "
                    + MediaType.JSON  + ", +json, "
                    + (Rdf.canRead().stream().map(MediaType::toString).collect(Collectors.joining(", ")))
                    + "]"
            ));*/
    }

    public void setFallbackContentType(MediaType fallbackContentType) {
        this.fallbackContentType = fallbackContentType;
    }

    private static DocumentReader<String> findReader(final MediaType type) {
        if (type == null) {
            return null;
        }

        if (JsonDocument.accepts(type)) {
            return s -> JsonDocument.of(type, s);
//            return Optional.of(is ->  JsonDocument.of(type, is));
        }

        if (RdfDocument.accepts(type)) {
            return s -> RdfDocument.of(type, s);
//            return Optional.of(is -> RdfDocument.of(type, is));
        }

        return null;
    }
}
