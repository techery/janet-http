package io.janet.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;

import io.janet.HttpActionClass;
import io.janet.compiler.utils.validation.ValidationError;
import io.janet.compiler.utils.validation.Validator;
import io.janet.http.annotations.Body;
import io.janet.http.annotations.HttpAction;

public class BodyValidator implements Validator<HttpActionClass> {
    @Override
    public Set<ValidationError> validate(HttpActionClass value) {
        Set<ValidationError> errors = new HashSet<ValidationError>();
        List<Element> annotations = value.getAllAnnotatedElements(Body.class);
        if (annotations.isEmpty()) return errors;
        Element element = annotations.get(0);

        if (value.getMethod().hasBody()) return errors;

        List<String> methodNames = new ArrayList<String>();
        for (HttpAction.Method method : HttpAction.Method.values()) {
            if (!method.hasBody()) continue;
            methodNames.add(method.name());
        }
        errors.add(new ValidationError(String.format("It's possible to use %s only with %s methods ", element, Body.class
                .getName(), methodNames.toString()), value.getTypeElement()));
        return errors;
    }
}
