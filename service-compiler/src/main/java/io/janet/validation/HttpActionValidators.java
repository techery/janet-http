package io.janet.validation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.io.File;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.janet.util.ElementResolver;
import io.janet.HttpActionClass;
import io.janet.body.ActionBody;
import io.janet.body.BytesArrayBody;
import io.janet.body.FileBody;
import io.janet.compiler.utils.validation.ValidationError;
import io.janet.compiler.utils.validation.Validator;
import io.janet.http.annotations.Body;
import io.janet.http.annotations.Field;
import io.janet.http.annotations.HttpAction;
import io.janet.http.annotations.Part;
import io.janet.http.annotations.ResponseHeader;
import io.janet.http.annotations.Status;
import io.janet.http.annotations.Url;
import io.janet.http.model.FormUrlEncodedRequestBody;
import io.janet.http.model.MultipartRequestBody;

public class HttpActionValidators implements Validator<HttpActionClass> {

    private final List<Validator<HttpActionClass>> validators;

    public HttpActionValidators(ElementResolver resolver) {
        validators = new ArrayList<Validator<HttpActionClass>>();
        //general rules
        validators.add(new FieldsModifiersValidator<HttpActionClass>(resolver));
        validators.add(new ParentsValidator());
        validators.add(new PathValidator());
        validators.add(new BodyValidator());
        validators.add(new RequestTypeValidator(Body.class, HttpAction.Type.SIMPLE));
        validators.add(new RequestTypeValidator(Field.class, HttpAction.Type.FORM_URL_ENCODED));
        validators.add(new RequestTypeValidator(Part.class, HttpAction.Type.MULTIPART));
        validators.add(new ResponseValidator());
        validators.add(new UrlValidator());
        //annotation rules
        validators.add(new AnnotationQuantityValidator(Body.class, 1));
        validators.add(new AnnotationQuantityValidator(Url.class, 1));
        validators.add(new AnnotationTypesValidator(ResponseHeader.class, String.class));
        validators.add(new AnnotationTypesValidator(Status.class, Boolean.class, Integer.class, Long.class, String.class, boolean.class, int.class, long.class));
        validators.add(new AnnotationTypesValidator(Part.class, MultipartRequestBody.PartBody.class, File.class, byte[].class, String.class, ActionBody.class,
                BytesArrayBody.class, MultipartRequestBody.class, FormUrlEncodedRequestBody.class, FileBody.class));
        validators.add(new AnnotationTypesValidator(Url.class, new Type[]{String.class, URI.class},
                new TypeName[]{ClassName.get("android.net", "Uri"), ClassName.get("okhttp3", "HttpUrl"), ClassName.get("com.squareup.okhttp", "HttpUrl")}));
    }

    @Override
    public Set<ValidationError> validate(HttpActionClass value) {
        Set<ValidationError> errors = new HashSet<ValidationError>();
        for (Validator<HttpActionClass> validator : validators) {
            errors.addAll(validator.validate(value));
        }
        return errors;
    }

}
