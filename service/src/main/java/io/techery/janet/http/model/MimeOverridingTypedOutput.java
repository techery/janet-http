package io.techery.janet.http.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.techery.janet.body.ActionBody;

public class MimeOverridingTypedOutput extends ActionBody {

    private final ActionBody delegate;

    public MimeOverridingTypedOutput(ActionBody delegate, String mimeType) {
        super(mimeType);
        if (delegate == null) throw new NullPointerException("Delegate is null");
        this.delegate = delegate;
    }

    @Override public long length() {
        return delegate.length();
    }

    @Override public InputStream getContent() throws IOException {
        return delegate.getContent();
    }

    @Override public void writeContentTo(OutputStream os) throws IOException {
        delegate.writeContentTo(os);
    }

}
