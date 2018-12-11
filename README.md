## HTTP ActionService
The REST service for [Janet](https://github.com/techery/janet) supports fully customizable requests, HTTP clients and converters.

### Getting Started
##### 1. Define a service and add it to `Janet`
```java
ActionService httpService = new HttpActionService(API_URL, new OkClient(), new GsonConverter(new Gson()))
Janet janet = new Janet.Builder().addService(httpService).build();
```

The service requires: endpoint URL, [HttpClient](clients) and [Converter](https://github.com/techery/janet-converters).
 
##### 2. Define a request/response action class
```java
@HttpAction(value = "/demo", method = HttpAction.Method.GET)
public class ExampleAction {
  @Response ExampleDataModel responseData;
}
```
Each action is an individual class that contains all information about the request and response.
It must be annotated with `@HttpAction`

##### 3. Use `ActionPipe` to send/observe an action
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

### HTTP Action Configuration

The request path, method and type are defined by the `@HttpAction` annotation:
* `value` –   url path
* `method` –  get/post/put/delete/head/patch
* `type` –    simple/multipart/form_url_encoded

To configure requests, action fields can be annotated with:
* `@Path` for a path value
* `@Url` to rewrite full URLs or only URL suffixes 
* `@Query` to request URL parameters
* `@Body` for the POST request body
* `@Field` for request fields if a request type is `HttpAction.Type.FORM_URL_ENCODED`
* `@Part` for multipart request parts
* `@RequestHeader` for request headers

To process responses, special annotations can be used:
* `@Response` to get the response body
* `@Status` to get the response status. The field types `Integer`, `Long`, `int` or `long` can be used to get the status code. Alternatively, you can use `boolean` to learn if a request was sent successfully
* `@ResponseHeader` to get response headers

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

### Advanced Bits
* supports request progress
* supports request cancelation
* provides useful `HttpException` for failed requests
* supports action inheritance 
* based on annotation processing
* consider using the javac option `'-Ajanet.http.factory.class.suffix=MyLib'` for API libraries

### Kotlin support
Kotlin action classes are supported except internal modifier. See [UsersAction](sample/src/main/java/io/techery/janet/http/sample/action/UsersAction.kt) as example.

### Download
```groovy
repositories {
    jcenter()
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.techery.janet-http:service:xxx'
    apt     'com.github.techery.janet-http:service-compiler:xxx'
    compile 'com.github.techery.janet-http:client-okhttp:xxx'
    compile 'com.github.techery.janet-converters:gson:yyy'
    // it is recommended you also explicitly depend on latest Janet version for bug fixes and new features.
    compile 'com.github.techery:janet:zzz' 
}
```
* janet: [![](https://jitpack.io/v/techery/janet.svg)](https://jitpack.io/#techery/janet)
* janet-http: [![](https://jitpack.io/v/techery/janet-http.svg)](https://jitpack.io/#techery/janet-http)
* janet-converters: [![](https://jitpack.io/v/techery/janet-converters.svg)](https://jitpack.io/#techery/janet-converters)

### Recipes
* Authorize requests via `ActionServiceWrapper`, e.g. [AuthWrapper](https://github.com/techery/janet-architecture-sample/blob/eff90f2f0a0013648263631a40bf3e76f7b9dfa2/app/src/main/java/io/techery/sample/service/AuthServiceWrapper.java)
* Log requests via `HttpClient` or `ActionServiceWrapper`, e.g. [SampleLoggingService](sample/src/main/java/io/techery/janet/http/sample/util/SampleLoggingService.java)
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
We put our effort to make it even more flexible and reusable, so everyone who loves `Retrofit` and a reactive approach should give it a try.

## License

    Copyright (c) 2016 Techery

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

