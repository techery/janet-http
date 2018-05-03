package io.janet.okhttp;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.internal.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.janet.body.ActionBody;
import io.janet.body.util.StreamUtil;
import io.janet.http.HttpClient;
import io.janet.http.internal.ProgressOutputStream;
import io.janet.http.internal.ProgressOutputStream.ProgressListener;
import io.janet.http.model.Header;
import io.janet.http.model.Request;
import io.janet.http.model.Response;
import io.janet.http.utils.RequestUtils;
import okio.BufferedSink;

public class OkClient implements HttpClient {

    private static OkHttpClient defaultOkHttp() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        client.setReadTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        return client;
    }

    private final com.squareup.okhttp.OkHttpClient client;

    public OkClient() {
        this(defaultOkHttp());
    }

    public OkClient(com.squareup.okhttp.OkHttpClient okHttpClient) {
        this.client = okHttpClient;
    }

    @Override public Response execute(Request request, final RequestCallback requestCallback) throws IOException {
        com.squareup.okhttp.Request.Builder okRequestBuilder = new com.squareup.okhttp.Request.Builder();
        okRequestBuilder.url(request.getUrl());
        for (Header header : request.getHeaders()) {
            okRequestBuilder.addHeader(header.getName(), header.getValue());
        }
        ActionRequestBody requestBody = null;
        final ActionBody actionBody = request.getBody();
        if (actionBody != null) {
            requestBody = new ActionRequestBody(actionBody, new ProgressListener() {
                @Override public void onProgressChanged(long bytesWritten) {
                    requestCallback.onProgress((int) ((bytesWritten * 100) / actionBody.length()));
                }
            });
        }
        com.squareup.okhttp.Request okRequest = okRequestBuilder.method(request.getMethod(), requestBody).build();
        RequestUtils.throwIfCanceled(request);
        Call call = client.newCall(okRequest);
        request.tag = call; //mark for cancellation
        com.squareup.okhttp.Response okResponse = call.execute();
        List<Header> responseHeaders = new ArrayList<Header>();
        for (String headerName : okResponse.headers().names()) {
            responseHeaders.add(new Header(headerName, okResponse.header(headerName)));
        }
        ActionBody responseBody = null;
        if (okResponse.body() != null) {
            responseBody = new ResponseActionBody(okResponse.body());
        }
        return new Response(
                okResponse.request().url().toString(),
                okResponse.code(), okResponse.message(), responseHeaders, responseBody
        );
    }

    @Override public void cancel(Request request) {
        if (request.tag != null && (request.tag instanceof Call)) {
            Call call = (Call) request.tag;
            call.cancel();
        }
        request.tag = RequestUtils.TAG_CANCELED;
    }

    private static class ActionRequestBody extends RequestBody {

        private final ActionBody actionBody;
        private final ProgressListener listener;

        private ActionRequestBody(ActionBody actionBody, ProgressListener progressListener) {
            this.actionBody = actionBody;
            this.listener = progressListener;
        }

        @Override public MediaType contentType() {
            return MediaType.parse(actionBody.mimeType());
        }

        @Override public void writeTo(BufferedSink sink) throws IOException {
            OutputStream stream = new ProgressOutputStream(sink.outputStream(), listener, HttpClient.PROGRESS_THRESHOLD);
            try {
                actionBody.writeContentTo(stream);
                stream.flush();
            } finally {
                Util.closeQuietly(stream);
            }
        }

        @Override public long contentLength() throws IOException {
            return actionBody.length();
        }
    }

    private static class ResponseActionBody extends ActionBody {

        private ResponseBody body;

        public ResponseActionBody(ResponseBody body) {
            super(body.contentType() == null ? null : body.contentType().toString());
            this.body = body;
        }

        @Override public long length() throws RuntimeException {
            try {
                return body.contentLength();
            } catch (IOException e) {
                throw new RuntimeException("Can't get content length from response", e);
            }
        }

        @Override public InputStream getContent() throws IOException {
            return body.byteStream();
        }

        @Override public void writeContentTo(OutputStream os) throws IOException {
            StreamUtil.writeAll(body.byteStream(), os, StreamUtil.NETWORK_CHUNK_SIZE);
        }
    }

}
