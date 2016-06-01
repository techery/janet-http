package io.techery.janet.http.sample.action;


import java.util.ArrayList;

import io.techery.janet.http.annotations.HttpAction;
import io.techery.janet.http.annotations.Query;
import io.techery.janet.http.annotations.Response;
import io.techery.janet.http.sample.action.base.BaseAction;
import io.techery.janet.http.sample.model.User;

@HttpAction("/users")
public class UsersAction extends BaseAction {

    @Query("since")
    final int since = 0;

    @Response
    ArrayList<User> response;

    public ArrayList<User> response() {
        return response;
    }

    @Override
    public String toString() {
        return "UsersAction{" +
                "since=" + since +
                ", response=" + response +
                '}';
    }
}
