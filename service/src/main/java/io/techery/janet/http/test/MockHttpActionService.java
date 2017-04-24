package io.techery.janet.http.test;

import io.techery.janet.*;
import io.techery.janet.body.ActionBody;
import io.techery.janet.converter.Converter;
import io.techery.janet.converter.ConverterException;
import io.techery.janet.http.HttpClient;
import io.techery.janet.http.model.Header;
import io.techery.janet.http.model.Request;
import rx.functions.Func1;
import rx.functions.Func2;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MockHttpActionService extends ActionServiceWrapper {

    private MockHttpActionService(List<Contract> contracts) {
        this(new HttpActionService("https://github.com/techery/janet", new MockClient(contracts), new MockConverter()));
    }

    private MockHttpActionService(ActionService actionService) {
        super(actionService);
    }

    public final static class Builder {

        private Func2<HttpClient, Converter, ActionService> actionServiceFunc = null;
        private final List<Contract> contracts = new ArrayList<>();

        public Builder bind(Response response, Func1<Request, Boolean> predicate) {
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
        public Builder wrapService(Func2<HttpClient, Converter, ActionService> actionServiceFunc) {
            this.actionServiceFunc = actionServiceFunc;
            return this;
        }

        public MockHttpActionService build() {
            if (actionServiceFunc == null) {
                return new MockHttpActionService(contracts);
            } else {
                return new MockHttpActionService(actionServiceFunc.call(new MockClient(contracts), new MockConverter()));
            }
        }
    }

    private final static class MockClient implements HttpClient {

        private final List<Contract> contracts;

        private MockClient(List<Contract> contracts) {
            this.contracts = contracts;
        }

        @Override
        public io.techery.janet.http.model.Response execute(Request request, RequestCallback requestCallback) throws IOException {
            for (Contract contract : contracts) {
                if (contract.predicate.call(request)) {
                    Response response = contract.response;
                    return new io.techery.janet.http.model.Response(request.getUrl(), response.status, response.reason, response.headers, new MockActionBody(response.body));
                }
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

        @Override public byte[] getContent() throws IOException {
            return new byte[0];
        }
    }

    private final static class Contract {
        private final Func1<Request, Boolean> predicate;
        private final Response response;

        private Contract(Func1<Request, Boolean> predicate, Response response) {
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
