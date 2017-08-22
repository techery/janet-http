package io.janet.http.exception;

import io.janet.converter.ConverterException;
import io.janet.http.model.Request;

/**
 * Thrown to indicate that something went wrong with converting http request.
 * This class includes target {@link Request}
 */
public class HttpSerializationException extends Exception {

    private final Request request;

    public HttpSerializationException(ConverterException cause, Request request) {
        super(cause);
        this.request = request;
    }

    public Request getRequest() {
        return request;
    }
}
