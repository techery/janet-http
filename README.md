## Http ActionService
REST service for [Janet](https://github.com/janet-io/janet). Supports fully customizable requests, http-clients and converters.

### Getting Started
##### 1. Define service and add it to `Janet`
```java
ActionService httpService = new HttpActionService(API_URL, new OkClient(), new GsonConverter(new Gson()))
Janet janet = new Janet.Builder().addService(httpService).build();
```

Service requires: End-point url, [HttpClient](clients) and [Converter](https://github.com/janet-io/janet-converters).
 
##### 2. Define Request-Response action class
```java
@HttpAction(value = "/demo", method = HttpAction.Method.GET)
public class ExampleAction {
  @Response ExampleDataModel responseData;
}
```
Each action is an individual class that contains all information about the request and response.
It must be annotated with `@HttpAction`

##### 3. Use `ActionPipe` to send/observe action
```java
ActionPipe<ExampleAction> actionPipe = janet.createPipe(ExampleAction.class);
actionPipe
  .createObservable(new ExampleAction())
  .subscribeOn(Schedulers.io())
  .subscribe(new ActionStateSubscriber<ExampleAction>()
          .onProgress((action, progress) -> System.out.println("Current progress: " + progress))
          .onSuccess(action -> System.out.println("Got example " + action))
          .onFail((action, throwable) -> System.err.println("Bad things happened " + throwable))
  );
```

### Http Action Configuration

Request path, method and type are defined by `@HttpAction` annotation:
* `value` –   url path
* `method` –  get/post/put/delete/head/patch
* `type` –    simple/multipart/form_url_encoded

To configure request, Action fields can be annotated with:
* `@Path` for path value
* `@Url` rewrites full url or suffixes 
* `@Query` for request URL parameters
* `@Body` for POST request body
* `@Field` for request fields if request type is `HttpAction.Type.FORM_URL_ENCODED`
* `@Part` for multipart request parts
* `@RequestHeader` for request headers

To process response, special annotations can be used:
* `@Response` for getting response body.
* `@Status` for getting response status. Field types `Integer`, `Long`, `int` or `long` can be used to get status code or use `boolean` to know that request was sent successfully
* `@ResponseHeader` for getting response headers

Example:
```java
@HttpAction(
        value = "/demo/{examplePath}/info",
        method = HttpAction.Method.GET,
        type = HttpAction.Type.SIMPLE
)
public class ExampleAction {
    // Request params
    @Path("examplePath") String pathValue;
    @Query("someParam") int queryParam;
    @RequestHeader("Example-RequestHeader-Name") String requestHeaderValue;
    // Response data
    @Status int statusCode;
    @Response ExampleDataModel exampleDataModel;
    @ResponseHeader("Example-ResponseHeader-Name") String responseHeaderValue;
}
```

### Advanced bits
* supports request progress;
* supports request cancelation;
* provides useful `HttpException` for failed requests;
* supports action inheritance 
* based on annotation processing
* consider using javac option `'-Ajanet.http.factory.class.suffix=MyLib'` for api libraries

### Kotlin support
Kotlin action classes are supported except `internal` modifier. See [TestProgressAction](sample/src/main/java/io/janet/http/sample/action/TestProgressAction.kt) as example. 

### Download
```groovy
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation 'com.github.janet-io.janet-http:service:xxx'
    apt     'com.github.janet-io.janet-http:service-compiler:xxx'
    implementation 'com.github.janet-io.janet-http:client-okhttp:xxx'
    implementation 'com.github.janet-io.janet-converters:gson:yyy'
    // it is recommended you also explicitly depend on latest Janet version for bug fixes and new features.
    implementation 'com.github.janet-io:janet:zzz' 
}
```
* janet: [![](https://jitpack.io/v/janet-io/janet.svg)](https://jitpack.io/#janet-io/janet)
* janet-http: [![](https://jitpack.io/v/janet-io/janet-http.svg)](https://jitpack.io/#janet-io/janet-http)
* janet-converters: [![](https://jitpack.io/v/janet-io/janet-converters.svg)](https://jitpack.io/#janet-io/janet-converters)

### Recipes
* Authorize requests via `ActionServiceWrapper`, e.g. [AuthWrapper](https://github.com/techery/janet-architecture-sample/blob/eff90f2f0a0013648263631a40bf3e76f7b9dfa2/app/src/main/java/io/techery/sample/service/AuthServiceWrapper.java)
* Log requests via `HttpClient` or `ActionServiceWrapper`, e.g. [SampleLoggingService](sample/src/main/java/io/janet/http/sample/util/SampleLoggingService.java)
* Convert `Retrofit` interfaces into actions with [Converter Util](https://github.com/techery/janet-retrofit-converter)
* Write tests using `MockHttpActionService`
* See more samples: 
[Simple Android app](https://github.com/techery/janet-http-android-sample),
[Advanced Android app](https://github.com/techery/janet-architecture-sample)

### Proguard
* Add [Rules](service/proguard-rules.pro) to your proguard config.
* See [Android Sample](https://github.com/techery/janet-http-android-sample) for complete proguard config example.

### Notes
`HttpActionService` is highly inspired by `Retrofit2` – thanks guys, it's awesome!
We put our effort to make it even more flexible and reusable, so everyone who loves `Retrofit` and reactive approach should give it a try.

## License

    Copyright (c) 2018 Techery

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

