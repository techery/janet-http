package io.techery.janet.http.sample;

import com.google.gson.Gson;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.techery.janet.ActionPipe;
import io.techery.janet.HttpActionService;
import io.techery.janet.Janet;
import io.techery.janet.gson.GsonConverter;
import io.techery.janet.helper.ActionStateSubscriber;
import io.techery.janet.http.sample.action.TestProgressAction;
import io.techery.janet.http.sample.action.UserReposAction;
import io.techery.janet.http.sample.action.UsersAction;
import io.techery.janet.http.sample.action.base.BaseAction;
import io.techery.janet.http.sample.model.User;
import io.techery.janet.http.sample.util.SampleLoggingService;
import io.techery.janet.okhttp3.OkClient;
import okhttp3.OkHttpClient;
import rx.Observable;

public class HttpSample {

    private static final String API_URL = "https://api.github.com";

    public static void main(String... args) throws NoSuchAlgorithmException, KeyManagementException {
        OkClient httpClient = new OkClient(createTrustingOkHttpClient());

        Janet janet = new Janet.Builder()
                .addService(new SampleLoggingService(new HttpActionService(API_URL, httpClient, new GsonConverter(new Gson()))))
                .build();

        ActionPipe<UsersAction> usersPipe = janet.createPipe(UsersAction.class);
        ActionPipe<UserReposAction> userReposPipe = janet.createPipe(UserReposAction.class);

        usersPipe.observeSuccess()
                .filter(BaseAction::isSuccess)
                .subscribe(
                        action -> System.out.println("received " + action),
                        System.err::println
                );

        usersPipe.send(new UsersAction());

        usersPipe.createObservable(new UsersAction())
                .filter(state -> state.action.isSuccess())
                .flatMap(state -> Observable.<User>from(state.action.getResponse()).first())
                .flatMap(user -> userReposPipe.createObservable(new UserReposAction(user.getLogin())))
                .subscribe(new ActionStateSubscriber<UserReposAction>()
                        .onSuccess(action -> System.out.println("repos request finished " + action))
                        .onFail((action, throwable) -> System.err.println("repos request exception " + throwable))
                );


        janet = new Janet.Builder()
                .addService(new SampleLoggingService(new HttpActionService("http://httpbin.org", httpClient, new GsonConverter(new Gson()))))
                .build();

        janet.createPipe(TestProgressAction.class)
                .createObservable(new TestProgressAction())
                .subscribe(new ActionStateSubscriber<TestProgressAction>()
                        .onSuccess(action -> System.out.println("request finished " + action))
                        .onProgress((action, progress) -> System.out.println(String.format("progress value:%s", progress))));

    }

    private static OkHttpClient createTrustingOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                    throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                    throws CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
        }};
        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(sslSocketFactory);
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        return builder.build();
    }
}
