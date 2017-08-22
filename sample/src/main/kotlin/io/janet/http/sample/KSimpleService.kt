package io.janet.http.sample

import com.google.gson.Gson
import io.janet.HttpActionService
import io.janet.Janet
import io.janet.gson.GsonConverter
import io.janet.helper.ActionStateSubscriber
import io.janet.http.sample.action.UserReposAction
import io.janet.http.sample.action.UsersAction
import io.janet.okhttp3.OkClient
import io.reactivex.Flowable

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
            .flatMap { Flowable.fromIterable(it.action.getResponse()).take(1) }
            .flatMap { userReposPipe.createObservable(UserReposAction(it.getLogin())) }
            .subscribe(ActionStateSubscriber<UserReposAction>()
                    .onSuccess { println("repos request finished $it") }
                    .onFail { _, t -> println("repos request exception $t") }
            )


}


