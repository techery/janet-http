package io.janet.http.sample.action;


import java.util.ArrayList;

import io.janet.http.annotations.HttpAction;
import io.janet.http.annotations.Path;
import io.janet.http.sample.action.base.BaseAction;
import io.janet.http.sample.model.Repository;
import io.janet.http.annotations.Response;

@HttpAction("/users/{login}/repos")
public class UserReposAction extends BaseAction {

    @Path("login") final String login;

    @Response ArrayList<Repository> repositories;

    public UserReposAction(String login) {
        this.login = login;
    }

    @Override public String toString() {
        return "UserReposAction{" +
                "login='" + login + '\'' +
                ", repositories=" + repositories +
                '}';
    }
}
