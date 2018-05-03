package io.janet.http.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import io.janet.body.ActionBody;


public final class MultipartRequestBody extends ActionBody {

    public static final String MIMETYPE_FORM_DATA = "multipart/form-data";
    public static final String DEFAULT_TRANSFER_ENCODING = "binary";

    private static final String DASH_DASH = "--";
    private static final String CRLF = "\r\n";

    private final List<MimePart> mimeParts = new LinkedList<MimePart>();

    private final String boundary;
    private final byte[] footer;
    private long length;

    public MultipartRequestBody() {
        this(UUID.randomUUID().toString());
    }

    MultipartRequestBody(String boundary) {
        super(MIMETYPE_FORM_DATA + "; boundary=" + boundary); //TODO add support for other mime types
        this.boundary = boundary;
        footer = buildBoundary(boundary, false, true);
        length = footer.length;
    }

    @Override
    public long length() {
        return length;
    }

    @Override
    public InputStream getContent() throws IOException {
        List<InputStream> streams = new ArrayList<InputStream>(mimeParts.size());
        for (MimePart mimePart : mimeParts) streams.add(mimePart.getContent());
        streams.add(new ByteArrayInputStream(footer));
        return new SequenceInputStream(Collections.enumeration(streams));
    }

    @Override public void writeContentTo(OutputStream os) throws IOException {
        for (MimePart part : mimeParts) part.writeTo(os);
        os.write(footer);
    }

    public void addPart(String name, String transferEncoding, PartBody body) {
        if (name == null) {
            throw new NullPointerException("Part name must not be null.");
        }
        if (transferEncoding == null) {
            transferEncoding = DEFAULT_TRANSFER_ENCODING;
        }
        if (body == null) {
            throw new NullPointerException("Part body must not be null.");
        }

        MimePart part = new MimePart(name, transferEncoding, body, boundary, mimeParts.isEmpty());
        mimeParts.add(part);

        long size = part.size();
        if (size == -1) {
            length = -1;
        } else if (length != -1) {
            length += size;
        }
    }

    List<byte[]> getParts() throws IOException {
        List<byte[]> parts = new ArrayList<byte[]>(mimeParts.size());
        for (MimePart part : mimeParts) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            part.writeTo(bos);
            parts.add(bos.toByteArray());
        }
        return parts;
    }

    public int getPartCount() {
        return mimeParts.size();
    }

    private static final class MimePart {
        private final String name;
        private final String transferEncoding;
        private final PartBody bodyWrapper;
        private final boolean isFirst;
        private final String boundary;

        private byte[] partBoundary;
        private byte[] partHeader;
        private boolean isBuilt;

        public MimePart(String name, String transferEncoding, PartBody bodyWrapper, String boundary, boolean isFirst) {
            this.name = name;
            this.transferEncoding = transferEncoding;
            this.bodyWrapper = bodyWrapper;
            this.isFirst = isFirst;
            this.boundary = boundary;
        }

        public InputStream getContent() throws IOException {
            build();
            List<InputStream> streams = Arrays.asList(
                    new ByteArrayInputStream(partBoundary),
                    new ByteArrayInputStream(partHeader),
                    bodyWrapper.body.getContent()
            );
            return new SequenceInputStream(Collections.enumeration(streams));
        }

        public void writeTo(OutputStream out) throws IOException {
            build();
            out.write(partBoundary);
            out.write(partHeader);
            bodyWrapper.body.writeContentTo(out);
        }

        public long size() {
            build();
            if (bodyWrapper.body.length() > -1) {
                return bodyWrapper.body.length() + partBoundary.length + partHeader.length;
            } else {
                return -1;
            }
        }

        private void build() {
            if (isBuilt) return;
            partBoundary = buildBoundary(boundary, isFirst, false);
            partHeader = buildHeader(name, transferEncoding, bodyWrapper.headers, bodyWrapper.body);
            isBuilt = true;
        }

        private static byte[] buildHeader(String name, String transferEncoding, List<Header> headers, ActionBody value) {
            try {
                StringBuilder result = new StringBuilder();

                result.append("Content-Disposition: form-data");
                result.append("; name=");
                appendQuotedString(result, name);
                for (Header header : headers) {
                    if (header.getName().equals("filename")) {
                        result.append("; filename=");
                        appendQuotedString(result, header.getValue());
                        break;
                    }
                }
                result.append(CRLF);

                result.append("Content-Type: ").append(value.mimeType()).append(CRLF);

                long length = value.length();
                if (length != -1) {
                    result.append("Content-Length: ").append(length).append(CRLF);
                }

                result.append("Content-Transfer-Encoding: ").append(transferEncoding).append(CRLF);

                // additional headers
                for (Header header : headers) {
                    if (header.getName().equals("filename")) continue;
                    result.append(header.toString()).append(CRLF);
                }

                result.append(CRLF);

                return result.toString().getBytes("UTF-8");
            } catch (IOException ex) {
                throw new RuntimeException("Unable to write multipart header", ex);
            }
        }

        static StringBuilder appendQuotedString(StringBuilder target, String key) {
            target.append('"');
            for (int i = 0, len = key.length(); i < len; i++) {
                char ch = key.charAt(i);
                switch (ch) {
                    case '\n':
                        target.append("%0A");
                        break;
                    case '\r':
                        target.append("%0D");
                        break;
                    case '"':
                        target.append("%22");
                        break;
                    default:
                        target.append(ch);
                        break;
                }
            }
            target.append('"');
            return target;
        }

    }

    public static class PartBody {
        public final ActionBody body;
        public final List<Header> headers;

        protected PartBody(ActionBody body, List<Header> headers) {
            if (body == null) throw new IllegalArgumentException("body can't be null");
            this.body = body;
            if (headers == null) headers = Collections.emptyList();
            this.headers = headers;
        }

        public static class Builder {
            private ActionBody body;
            private List<Header> headers;

            public Builder setBody(ActionBody body) {
                this.body = body;
                return this;
            }

            public Builder addHeader(String name, String value) {
                if (headers == null) headers = new ArrayList<Header>();
                headers.add(new Header(name, value));
                return this;
            }

            public PartBody build() {
                return new PartBody(body, headers);
            }
        }
    }

    private static byte[] buildBoundary(String boundary, boolean first, boolean last) {
        try {
            StringBuilder sb = new StringBuilder(boundary.length() + 8);

            if (!first) {
                sb.append(CRLF);
            }
            sb.append(DASH_DASH);
            sb.append(boundary);
            if (last) {
                sb.append(DASH_DASH);
            }
            sb.append(CRLF);
            return sb.toString().getBytes("UTF-8");
        } catch (IOException ex) {
            throw new RuntimeException("Unable to write multipart boundary", ex);
        }
    }
}
