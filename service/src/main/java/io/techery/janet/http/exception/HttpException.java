package io.techery.janet.http.exception;


import java.net.SocketTimeoutException;

import io.techery.janet.http.model.Request;
import io.techery.janet.http.model.Response;

/**
 * Exception to indicate Http Request/Response fail.
 * <ul>
 *     <li> Fail on response (status code is out of 2xx) will contain both {@link Request} and {@link Response} models.</li>
 *     <li> Fail on request (e.g. {@link SocketTimeoutException}) will contain {@link Request} and {@link Throwable} cause.</li>
 * </ul>
 *
 * See {@code isFailedOnRequest}, getRequest, getResponse, getCause for details.
 */
public class HttpException extends Exception {

    private final Request request;
    private final Response response;

    public static HttpException forRequest(Request request, Throwable cause) {
        return new HttpException(request, null, cause);
    }

    public static HttpException forResponse(Request request, Response response) {
        return new HttpException(request, response, null);
    }

    private HttpException(Request request, Response response, Throwable cause) {
        super(createMessage(request, response), cause);
        this.request = request;
        this.response = response;
    }

    private static String createMessage(Request request, Response response) {
        return response == null ?
                "HTTP call failed on request" :
                "HTTP call failed on response with status=" + response.getStatus() + ", with reason=" + response.getReason();
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public boolean isRequestFail() {
        return response == null;
    }
}
