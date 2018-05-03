package io.techery.janet.http.sample.action


import java.util.ArrayList

import io.techery.janet.http.annotations.HttpAction
import io.techery.janet.http.annotations.Query
import io.techery.janet.http.annotations.Response
import io.techery.janet.http.sample.action.base.BaseAction
import io.techery.janet.http.sample.model.User

@HttpAction("/users")
data class UsersAction(@Query("since") val since: Int = 0) : BaseAction() {

    @Response
    lateinit var response: ArrayList<User>

    override fun toString(): String {
        return "UsersAction(since=$since${if (::response.isInitialized) ", response=$response" else ""})"
    }

}
