package io.janet.http.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.janet.ActionHolder;
import io.janet.ActionService;
import io.janet.ActionServiceWrapper;
import io.janet.JanetException;
import io.janet.body.ActionBody;
import io.janet.converter.Converter;
import io.janet.converter.ConverterException;
import io.janet.http.HttpClient;
import io.janet.http.model.Header;
import io.janet.http.model.Request;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Predicate;
import io.janet.HttpActionService;

public final class MockHttpActionService extends ActionServiceWrapper {

    private MockHttpActionService(List<Contract> contracts) {
        this(new HttpActionService("https://github.com/techery/janet", new MockClient(contracts), new MockConverter()));
    }

    private MockHttpActionService(ActionService actionService) {
        super(actionService);
    }

    public final static class Builder {

        private BiFunction<HttpClient, Converter, ActionService> actionServiceFunc = null;
        private final List<Contract> contracts = new ArrayList<Contract>();

        public Builder bind(Response response, Predicate<Request> predicate) {
            if (response == null) {
                throw new IllegalArgumentException("response == null");
            }
            if (predicate == null) {
                throw new IllegalArgumentException("predicate == null");
            }
            contracts.add(new Contract(predicate, response));
            return this;
        }

        /**
         * In case if you want to build your testing around some real service (e.g. if it does some additional logic,
         * sufficient for your needs) - instantiate and return it withing given Func2, passing forward two parameters:
         * HttpClient and Converter
         *
         * @param actionServiceFunc func that will instantiate service to wrap in a predefined way
         * @return current instance of Builder
         */
        public Builder wrapService(BiFunction<HttpClient, Converter, ActionService> actionServiceFunc) {
            this.actionServiceFunc = actionServiceFunc;
            return this;
        }

        public MockHttpActionService build() {
            if (actionServiceFunc == null) {
                return new MockHttpActionService(contracts);
            } else {
                try {
                    ActionService service = actionServiceFunc.apply(new MockClient(contracts), new MockConverter());
                    return new MockHttpActionService(service);
                } catch (Exception e) { return null; }
            }
        }
    }

    private final static class MockClient implements HttpClient {

        private final List<Contract> contracts;

        private MockClient(List<Contract> contracts) {
            this.contracts = contracts;
        }

        @Override
        public io.janet.http.model.Response execute(Request request, RequestCallback requestCallback) throws IOException {
            for (Contract contract : contracts) {
                try {
                    if (contract.predicate.test(request)) {
                        Response response = contract.response;
                        return new io.janet.http.model.Response(request.getUrl(), response.status, response.reason, response.headers, new MockActionBody(response.body));
                    }
                } catch (Exception e) {}
            }
            throw new UnsupportedOperationException("There is no contract for " + request);
        }

        @Override public void cancel(Request request) {}
    }

    private final static class MockConverter implements Converter {

        @Override public Object fromBody(ActionBody body, Type type) throws ConverterException {
            if (body instanceof MockActionBody) {
                return ((MockActionBody) body).body;
            }
            throw new UnsupportedOperationException("Something went happened with mock response. Couldn't convert " + body);
        }

        @Override public ActionBody toBody(Object object) throws ConverterException {
            return new EmptyActionBody();
        }
    }

    private final static class MockActionBody extends EmptyActionBody {
        private final Object body;

        private MockActionBody(Object body) {
            this.body = body;
        }
    }

    private static class EmptyActionBody extends ActionBody {

        public EmptyActionBody() {
            super(null);
        }

        @Override public long length() {
            return 0L;
        }

        @Override public InputStream getContent() throws IOException {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override public void writeContentTo(OutputStream os) throws IOException {
        }
    }

    private final static class Contract {
        private final Predicate<Request> predicate;
        private final Response response;

        private Contract(Predicate<Request> predicate, Response response) {
            this.predicate = predicate;
            this.response = response;
        }
    }

    public static class Response {
        private final int status;
        private final String reason;
        private final List<Header> headers;
        private Object body;

        public Response(int status) {
            this.status = status;
            this.reason = "";
            this.headers = new ArrayList<Header>();
        }

        public Response body(Object body) {
            if (body == null) {
                throw new IllegalArgumentException("body == null");
            }
            this.body = body;
            return this;
        }

        public Response addHeader(Header... headers) {
            if (headers == null) {
                throw new IllegalArgumentException("headers == null");
            }
            Collections.addAll(this.headers, headers);
            return this;
        }

        public Response reason(String reason) {
            if (reason == null) {
                throw new IllegalArgumentException("reason == null");
            }
            return this;
        }
    }

    @Override protected <A> boolean onInterceptSend(ActionHolder<A> holder) throws JanetException {
        return false;
    }

    @Override protected <A> void onInterceptCancel(ActionHolder<A> holder) {}

    @Override protected <A> void onInterceptStart(ActionHolder<A> holder) {}

    @Override protected <A> void onInterceptProgress(ActionHolder<A> holder, int progress) {}

    @Override protected <A> void onInterceptSuccess(ActionHolder<A> holder) {}

    @Override protected <A> boolean onInterceptFail(ActionHolder<A> holder, JanetException e) {
        return false;
    }
}
