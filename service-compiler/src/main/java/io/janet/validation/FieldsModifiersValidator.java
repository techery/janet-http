package io.janet.validation;

import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import io.janet.util.ElementResolver;
import io.janet.compiler.utils.ActionClass;
import io.janet.compiler.utils.validation.ValidationError;
import io.janet.compiler.utils.validation.Validator;

class FieldsModifiersValidator<T extends ActionClass> implements Validator<T> {

    private final ElementResolver resolver;

    FieldsModifiersValidator(ElementResolver resolver) {this.resolver = resolver;}

    @Override
    public Set<ValidationError> validate(T value) {
        Set<ValidationError> messages = new HashSet<ValidationError>();
        for (Element element : value.getAllAnnotatedMembers()) {
            if (element.getKind() != ElementKind.FIELD) continue;
            boolean hasPrivateModifier = element.getModifiers().contains(Modifier.PRIVATE);
            boolean hasStaticModifier = element.getModifiers().contains(Modifier.STATIC);
            if (resolver.isKotlinClass(value.getTypeElement())) {
                if (!isKotlinValid(value.getTypeElement(), element)) {
                    messages.add(new ValidationError("Annotated fields must have public accessors", element));
                }
            } else if (hasStaticModifier || hasPrivateModifier) {
                messages.add(new ValidationError("Annotated fields can't be static or private", element));
            }
        }
        return messages;
    }

    private boolean isKotlinValid(TypeElement classElement, Element element) {
        return resolver.hasAccessorByField(classElement, element);
    }

}
