package io.techery.janet.validation;

import java.util.Collections;
import java.util.Set;

import io.techery.janet.HttpActionClass;
import io.techery.janet.compiler.utils.validation.ValidationError;
import io.techery.janet.compiler.utils.validation.Validator;

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
