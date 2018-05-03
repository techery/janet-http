package io.janet.validation;

import org.apache.commons.lang.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Element;

import io.janet.HttpActionClass;
import io.janet.compiler.utils.validation.ValidationError;
import io.janet.compiler.utils.validation.Validator;
import io.janet.http.annotations.Url;

public class UrlValidator implements Validator<HttpActionClass> {
    @Override
    public Set<ValidationError> validate(HttpActionClass value) {
        Set<ValidationError> errors = new HashSet<ValidationError>();
        ValidationError error = validateInternal(value);
        if (error != null) {
            errors.add(error);
        }
        return errors;
    }

    private static ValidationError validateInternal(HttpActionClass value) {
        List<Element> annotatedElements = value.getAllAnnotatedElements(Url.class);
        if (!annotatedElements.isEmpty() && !StringUtils.isEmpty(value.getPath())) {
            return new ValidationError("@Url can't be used with specified path (@HttpAction.value())", annotatedElements
                    .get(0));
        }
        return null;
    }
}
