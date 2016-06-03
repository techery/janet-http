package io.techery.janet.http.sample.action.base;


import io.techery.janet.http.annotations.Status;

/**
 * This action class was created to show,
 * that action helper will be generated to fill the
 * annotated variables of super class too.
 */
public abstract class BaseAction<T> extends ServerAction<T>{

    @Status
    boolean success;

    public boolean isSuccess() {
        return success;
    }
}
