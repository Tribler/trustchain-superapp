package com.apicatalog.jsonld.http;

import android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.JsonLdErrorCode;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class DefaultHttpClient implements HttpClient {

     private static final OkHttpClient CLIENT = new OkHttpClient.Builder().followRedirects(false).build();

    private static final DefaultHttpClient INSTANCE = new DefaultHttpClient(CLIENT);

    private final OkHttpClient httpClient;

    public DefaultHttpClient(final OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public HttpResponse send(URI targetUri, String requestProfile) throws JsonLdError {

        Request request = new Request.Builder()
            .url(targetUri.toString())
            .addHeader("Accept", requestProfile)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {

            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            // Get response body
//            System.out.println(response.body().string());

            return new HttpResponseImpl(response);
        } catch (IOException e) {
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, e);
        }

        /*HttpRequest request =
            HttpRequest.newBuilder()
                .GET()
                .uri(targetUri)
                .header("Accept", requestProfile)
                .build();

        try {
            return new HttpResponseImpl(httpClient.send(request, BodyHandlers.ofInputStream()));

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, e);

        } catch (IOException e) {

            throw new JsonLdError(JsonLdErrorCode.LOADING_DOCUMENT_FAILED, e);
        }*/
    }

    public static final HttpClient defaultInstance() {
        return INSTANCE;
    }

    public static class HttpResponseImpl implements HttpResponse {

        private final Response response;
        private String responseBody;

        /*HttpResponseImpl(java.net.http.HttpResponse<InputStream> response) {
            this.response = response;
        }*/

        HttpResponseImpl(Response response) {
            this.response = response;
            try {
                responseBody = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public int statusCode() {
            return response.code();
        }

        @Override
        public String body() {
            return responseBody;
            /*try {
                return Objects.requireNonNull(response.body()).string();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }*/
        }

        @Override
        public Collection<String> links() {
            return response.headers().toMultimap().get("link");
//            return response.headers().map().get("link");
        }

        @Override
        public String contentType() {
            List<String> contentTypes = response.headers().toMultimap().get("content-type");
            return contentTypes.get(0);
//            return response.headers().firstValue("content-type");
        }

        @Override
        public String location() {
            List<String> locations = response.headers().toMultimap().get("location");
            return locations.get(0);
//            return response.headers().firstValue("location");
        }

        @Override
        public void close() { /* unused */ }
    }
}
