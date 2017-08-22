package io.janet.http.exception;

import io.janet.converter.ConverterException;
import io.janet.http.model.Response;

/**
 * Thrown to indicate that something went wrong with converting http response.
 * This class includes target {@link Response}
 */
public class HttpDeserializationException extends Exception {

    private final Response response;

    public HttpDeserializationException(ConverterException cause, Response response) {
        super(cause);
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }
}
