package io.techery.janet.http.sample

import com.google.gson.Gson
import io.techery.janet.HttpActionService
import io.techery.janet.Janet
import io.techery.janet.gson.GsonConverter
import io.techery.janet.helper.ActionStateSubscriber
import io.techery.janet.http.sample.action.UserReposAction
import io.techery.janet.http.sample.action.UsersAction
import io.techery.janet.okhttp3.OkClient
import rx.Observable

const private val API_URL = "https://api.github.com"


fun main(args: Array<String>) {

    val janet = Janet.Builder()
            .addService(HttpActionService(API_URL, OkClient(), GsonConverter(Gson())))
            .build()

    val usersPipe = janet.createPipe(UsersAction::class.java)
    val userReposPipe = janet.createPipe(UserReposAction::class.java)

    usersPipe.observeSuccess()
            .filter({ it.isSuccess() })
            .subscribe({ println("received $it") }) { println(it) }

    usersPipe.createObservable(UsersAction())
            .filter { it.action.isSuccess }
            .flatMap { Observable.from(it.action.getResponse()).first() }
            .flatMap { userReposPipe.createObservable(UserReposAction(it.getLogin())) }
            .subscribe(ActionStateSubscriber<UserReposAction>()
                    .onSuccess { println("repos request finished $it") }
                    .onFail { a, t -> println("repos request exception $t") }
            )


}


