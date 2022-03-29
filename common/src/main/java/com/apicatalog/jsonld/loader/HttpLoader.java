package com.apicatalog.jsonld.loader;

import com.apicatalog.jsonld.http.DefaultHttpClient;
import com.apicatalog.jsonld.http.HttpClient;

import okhttp3.OkHttpClient;

public class HttpLoader extends DefaultHttpLoader {

    private static final HttpLoader INSTANCE = new HttpLoader(DefaultHttpClient.defaultInstance());

//    public HttpLoader(OkHttpClient httpClient) {
//        this(httpClient, MAX_REDIRECTIONS);
//    }

    public HttpLoader(OkHttpClient httpClient, int maxRedirections) {
        this(new DefaultHttpClient(httpClient), maxRedirections);
    }

    public HttpLoader(HttpClient httpClient) {
        super(httpClient);
    }

    public HttpLoader(HttpClient httpClient, int maxRedirections) {
        super(httpClient, maxRedirections);
    }

    public static final DocumentLoader defaultInstance() {
        return INSTANCE;
    }
}
