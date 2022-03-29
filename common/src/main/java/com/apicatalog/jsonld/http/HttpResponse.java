package com.apicatalog.jsonld.http;

import java.io.Closeable;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

public interface HttpResponse extends Closeable {

    int statusCode();

    String body();

    Collection<String> links();

    String contentType();

    String location();

}
