package io.techery.janet.http.sample.action;


import java.util.ArrayList;

import io.techery.janet.http.annotations.HttpAction;
import io.techery.janet.http.annotations.Path;
import io.techery.janet.http.annotations.Response;
import io.techery.janet.http.sample.action.base.BaseAction;
import io.techery.janet.http.sample.model.Repository;

@HttpAction("/users/{login}/repos")
public class UserReposAction extends BaseAction<ArrayList<Repository>> {

    @Path("login")
    final String login;

    @Response
    ArrayList<Repository> repositories;

    public UserReposAction(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public ArrayList<Repository> getRepositories() {
        return repositories;
    }

    @Override
    public String toString() {
        return "UserReposAction{" +
                "login='" + login + '\'' +
                ", repositories=" + repositories +
                '}';
    }

    @Override public ArrayList<Repository> getResponse() {
        return repositories;
    }
}
