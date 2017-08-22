package io.janet.http.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;

import io.janet.body.ActionBody;
import io.janet.body.util.StreamUtil;


public final class FormUrlEncodedRequestBody extends ActionBody {

    private static final String MIMETYPE = "application/x-www-form-urlencoded; charset=UTF-8";

    final ByteArrayOutputStream content = new ByteArrayOutputStream();

    public FormUrlEncodedRequestBody() {
        super(MIMETYPE);
    }

    public void addField(String name, String value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        if (content.size() > 0) {
            content.write('&');
        }
        try {
            name = URLEncoder.encode(name, "UTF-8");
            value = URLEncoder.encode(value, "UTF-8");

            content.write(name.getBytes("UTF-8"));
            content.write('=');
            content.write(value.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public long length() {
        return content.size();
    }

    @Override public InputStream getContent() throws IOException {
        return new ByteArrayInputStream(content.toByteArray());
    }

    @Override public void writeContentTo(OutputStream os) throws IOException {
        StreamUtil.writeAll(getContent(), os, StreamUtil.NETWORK_CHUNK_SIZE);
    }

}
