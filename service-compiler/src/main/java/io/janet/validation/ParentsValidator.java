package io.janet.validation;

import java.util.Collections;
import java.util.Set;

import io.janet.HttpActionClass;
import io.janet.compiler.utils.validation.ValidationError;
import io.janet.compiler.utils.validation.Validator;

public class ParentsValidator implements Validator<HttpActionClass> {
    @Override public Set<ValidationError> validate(HttpActionClass value) {
        while (value.getParent() != null) {
            value = value.getParent();
            if (value.isAnnotatedClass()) {
                return Collections.singleton(new ValidationError("Parent class cant't be annotated with @HttpAction", value
                        .getTypeElement()));
            }
        }
        return Collections.emptySet();
    }
}
