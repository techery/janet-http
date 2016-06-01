package io.techery.janet;

import com.squareup.javapoet.ClassName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import io.techery.janet.compiler.utils.ActionClass;
import io.techery.janet.http.annotations.HttpAction;

public class HttpActionClass extends ActionClass {
    private HttpAction.Method method;
    private String path;
    private HttpAction.Type requestType;
    private HttpActionClass parent;

    public HttpActionClass(Elements elementUtils, TypeElement typeElement, HttpActionClass parent) {
        super(HttpAction.class, elementUtils, typeElement);
        this.parent = parent;
        HttpAction annotation = typeElement.getAnnotation(HttpAction.class);
        if (annotation != null) {
            method = annotation.method();
            path = annotation.value();
            requestType = annotation.type();
        }
    }

    public boolean isAnnotatedClass() {
        return method != null && path != null && requestType != null;
    }

    public HttpActionClass getParent() {
        return parent;
    }

    public HttpAction.Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public HttpAction.Type getRequestType() {
        return requestType;
    }

    public ClassName getHelperName() {
        return ClassName.get(getPackageName(), getTypeElement().getSimpleName() + HttpHelpersGenerator.HELPER_SUFFIX);
    }

    @Override public List<Element> getAnnotatedElements(Class annotationClass) {
        List<Element> elements = super.getAnnotatedElements(annotationClass);
        for (Iterator<Element> iterator = elements.iterator(); iterator.hasNext(); ) {
            Element element = iterator.next();
            if (!element.getEnclosingElement().equals(getTypeElement())) {
                iterator.remove();
            }
        }
        return elements;
    }

    public List<Element> getAllAnnotatedElements(Class annotationClass) {
        List<Element> elements = new ArrayList<Element>(getAnnotatedElements(annotationClass));
        if (parent != null) {
            elements.addAll(parent.getAllAnnotatedElements(annotationClass));
        }
        return elements;
    }

    @Override public String toString() {
        return getTypeElement() + "{" +
                "method=" + method +
                ", path='" + path + '\'' +
                ", requestType=" + requestType +
                ", parent=" + parent +
                '}';
    }
}
