package io.janet.http.sample.action;


import java.util.ArrayList;

import io.janet.http.annotations.HttpAction;
import io.janet.http.annotations.Query;
import io.janet.http.annotations.Response;
import io.janet.http.sample.action.base.BaseAction;
import io.janet.http.sample.model.User;

@HttpAction("/users")
public class UsersAction extends BaseAction {

    @Query("since") final int since = 0;

    @Response ArrayList<User> response;

    public ArrayList<User> getResponse() {
        return response;
    }

    @Override public String toString() {
        return "UsersAction{" +
                "since=" + since +
                ", response=" + response +
                '}';
    }
}
