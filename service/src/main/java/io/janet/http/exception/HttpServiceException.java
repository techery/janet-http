package io.janet.http.exception;


import io.janet.JanetException;

public class HttpServiceException extends JanetException {

    public HttpServiceException(Throwable cause) {
        super(cause);
    }

    public HttpServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
