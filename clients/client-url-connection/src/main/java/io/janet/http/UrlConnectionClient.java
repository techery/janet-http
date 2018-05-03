package io.janet.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.janet.body.ActionBody;
import io.janet.body.util.StreamUtil;
import io.janet.http.internal.ProgressOutputStream;
import io.janet.http.model.Header;
import io.janet.http.model.Request;
import io.janet.http.model.Response;
import io.janet.http.utils.RequestUtils;


public class UrlConnectionClient implements HttpClient {
    private static final int CHUNK_SIZE = 4096;
    private static final int BUFFER_SIZE = 0x1000;

    public UrlConnectionClient() {}

    @Override public Response execute(Request request, RequestCallback requestCallback) throws IOException {
        HttpURLConnection connection = openConnection(request);
        RequestUtils.throwIfCanceled(request);
        request.tag = connection;
        writeRequest(connection, request, requestCallback);
        RequestUtils.throwIfCanceled(request);
        return readResponse(connection);
    }

    @Override public void cancel(Request request) {
        if (request.tag != null && (request.tag instanceof HttpURLConnection)) {
            HttpURLConnection connection = (HttpURLConnection) request.tag;
            connection.disconnect();
            if (connection.getDoOutput()) {
                try {
                    connection.getOutputStream().close();
                } catch (IOException ignored) {}
            }
            if (connection.getDoInput()) {
                try {
                    connection.getInputStream().close();
                } catch (IOException ignored) {}
            }
        }
        request.tag = RequestUtils.TAG_CANCELED; //mark request as canceled
    }

    protected HttpURLConnection openConnection(Request request) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(request.getUrl()).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        return connection;
    }

    private void writeRequest(HttpURLConnection connection, Request request, final RequestCallback requestCallback) throws IOException {
        connection.setRequestMethod(request.getMethod());
        connection.setDoInput(true);

        for (Header header : request.getHeaders()) {
            connection.addRequestProperty(header.getName(), header.getValue());
        }

        ActionBody body = request.getBody();
        if (body != null) {
            connection.setDoOutput(true);
            connection.addRequestProperty("Content-Type", body.mimeType());
            OutputStream outputStream;
            final long length = body.length();
            if (length != -1) {
                connection.setFixedLengthStreamingMode((int) length);
                connection.addRequestProperty("Content-Length", String.valueOf(length));
                outputStream = new ProgressOutputStream(connection.getOutputStream(), new ProgressOutputStream.ProgressListener() {
                    @Override public void onProgressChanged(long bytesWritten) {
                        requestCallback.onProgress((int) ((bytesWritten * 100) / length));
                    }
                });
            } else {
                outputStream = connection.getOutputStream();
                connection.setChunkedStreamingMode(CHUNK_SIZE);
            }
            body.writeContentTo(outputStream);
        }
    }

    Response readResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        String reason = connection.getResponseMessage();
        if (reason == null) reason = ""; // HttpURLConnection treats empty reason as null.

        List<Header> headers = new ArrayList<Header>();
        for (Map.Entry<String, List<String>> field : connection.getHeaderFields().entrySet()) {
            String name = field.getKey();
            for (String value : field.getValue()) {
                headers.add(new Header(name, value));
            }
        }
        ActionBody responseBody = new ResponseActionBody(connection);
        return new Response(connection.getURL().toString(), status, reason, headers, responseBody);
    }

    private static class ResponseActionBody extends ActionBody {

        private HttpURLConnection body;

        public ResponseActionBody(HttpURLConnection body) {
            super(body.getContentType());
            this.body = body;
        }

        @Override public long length() {
            return body.getContentLengthLong();
        }

        @Override public InputStream getContent() throws IOException {
            int status = body.getResponseCode();
            if (status >= 200 && status < 300) {
                return body.getInputStream();
            } else {
                return body.getErrorStream();
            }
        }

        @Override public void writeContentTo(OutputStream os) throws IOException {
            StreamUtil.writeAll(getContent(), os, StreamUtil.NETWORK_CHUNK_SIZE);
        }
    }

}
