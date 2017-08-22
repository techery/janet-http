package io.janet.okhttp3;

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
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;
import okio.BufferedSink;

public class OkClient implements HttpClient {

    private static OkHttpClient defaultOkHttp() {
        return new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .build();
    }

    private final OkHttpClient client;

    public OkClient() {
        this(defaultOkHttp());
    }

    public OkClient(OkHttpClient okHttpClient) {
        this.client = okHttpClient;
    }

    @Override public Response execute(Request request, final RequestCallback requestCallback) throws IOException {
        okhttp3.Request.Builder okRequestBuilder = new okhttp3.Request.Builder();
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
        final okhttp3.Request okRequest = okRequestBuilder.method(request.getMethod(), requestBody).build();
        RequestUtils.throwIfCanceled(request);
        Call call = client.newCall(okRequest);
        request.tag = call; //mark for cancellation
        final okhttp3.Response okResponse = call.execute();
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

    private static class ActionRequestBody extends okhttp3.RequestBody {

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

        private final ResponseBody body;

        public ResponseActionBody(ResponseBody body) {
            super(body.contentType() == null ? null : body.contentType().toString());
            this.body = body;
        }

        @Override public long length() {
            return body.contentLength();
        }

        @Override public InputStream getContent() throws IOException {
            return body.byteStream();
        }

        @Override public void writeContentTo(OutputStream os) throws IOException {
            StreamUtil.writeAll(body.byteStream(), os, StreamUtil.NETWORK_CHUNK_SIZE);
        }
    }

}
