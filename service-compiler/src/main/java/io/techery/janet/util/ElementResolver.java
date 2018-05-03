package io.techery.janet.util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;


public class ElementResolver {

    private final Elements elementUtils;

    public ElementResolver(Elements elementUtils) {
        this.elementUtils = elementUtils;
    }

    public boolean isKotlinClass(TypeElement typeElement) {
        // simpler check is possible (getAnnotation(kotlin.Metadata.class) != null) but we do not want to add kotlin dependency
        boolean isKotlinClass = false;
        for (AnnotationMirror annotationMirror : elementUtils.getAllAnnotationMirrors(typeElement)) {
            if (((TypeElement) annotationMirror.getAnnotationType().asElement()).getQualifiedName()
                    .toString()
                    .equals("kotlin.Metadata"))
                isKotlinClass = true;
        }

        return isKotlinClass;
    }

    public boolean checkHasAnnotatedField(Class<? extends Annotation> annotationClass, TypeElement typeElement) {
        boolean hasAnnotatedField = false;

        for (Element element : elementUtils.getAllMembers(typeElement)) {
            if (element.getKind() == ElementKind.FIELD) {
                if (element.getAnnotation(annotationClass) != null) {
                    hasAnnotatedField = true;
                }
            }
        }

        return hasAnnotatedField;
    }

    public Set<String> getAnnotatedFieldNames(Class<? extends Annotation> annotationClass, TypeElement typeElement) {
        Set<String> names = new HashSet<String>();
        for (Element element : elementUtils.getAllMembers(typeElement)) {
            if (element.getKind() == ElementKind.FIELD) {
                if (element.getAnnotation(annotationClass) != null) {
                    names.add(resolveAccessibleFieldNameToRead(typeElement, element));
                }
            }
        }

        return names;
    }

    public boolean hasAccessorByField(TypeElement typeElement, Element element) {
        if (element.getKind() != ElementKind.FIELD) throw new IllegalArgumentException("Element must be field");
        if (!isKotlinClass(typeElement)) return true;
        String getterByFieldName = String.format(Locale.US, "get%s", capitalize(element.getSimpleName().toString()));
        for (Element e : elementUtils.getAllMembers(typeElement)) {
            if (e.getKind() != ElementKind.METHOD) continue;
            if (e.getSimpleName().toString().equals(getterByFieldName)) return true;
        }
        return false;
    }

    public String resolveAccessibleFieldNameToRead(TypeElement typeElement, Element element) {
        if (element.getKind() != ElementKind.FIELD) throw new IllegalArgumentException("Element must be field");
        final String fieldName = element.getSimpleName().toString();
        if (isKotlinClass(typeElement)) {
            if (Pattern.compile("^is[A-Z]+").matcher(fieldName).matches()) {
                return fieldName + "()";
            } else {
                Element methodElement = null;
                String methodByFieldName = String.format(Locale.US, "get%s", capitalize(fieldName));
                for (Element e : elementUtils.getAllMembers(typeElement)) {
                    if (e.getKind() == ElementKind.METHOD && e.getSimpleName().toString()
                            .startsWith(methodByFieldName)) {
                        methodElement = e;
                        break;
                    }
                }
                return methodElement.getSimpleName().toString() + "()";
            }
        } else {
            return fieldName;
        }
    }

    public String resolveAccessibleFieldNameToWrite(TypeElement typeElement, Element element, String value) {
        if (element.getKind() != ElementKind.FIELD) throw new IllegalArgumentException("Element must be field");
        final String fieldName = element.getSimpleName().toString();
        if (isKotlinClass(typeElement)) {
            Element methodElement = null;
            String methodByFieldName = String.format(Locale.US, "set%s", capitalize(fieldName));
            for (Element e : elementUtils.getAllMembers(typeElement)) {
                if (e.getKind() == ElementKind.METHOD && e.getSimpleName().toString()
                        .startsWith(methodByFieldName)) {
                    methodElement = e;
                    break;
                }
            }
            return String.format(Locale.US, "%s(%s)", methodElement.getSimpleName()
                    .toString()
                    .replace("$", "$$"), value);
        } else {
            return fieldName + " = " + value;
        }
    }

    public TypeElement getSuperclass(TypeElement typeElement) {
        TypeMirror superTypeMirror = typeElement.getSuperclass();

        if (superTypeMirror instanceof NoType) {
            return null;
        }

        TypeElement superTypeElement =
                (TypeElement) ((DeclaredType) superTypeMirror).asElement();

        if (superTypeElement.getQualifiedName().toString().equals(Object.class.getCanonicalName())) {
            return null;
        }

        return superTypeElement;
    }

    public static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

}
