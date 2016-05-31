package io.techery.janet;

import com.google.auto.service.AutoService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import io.techery.janet.compiler.utils.validation.ClassValidator;
import io.techery.janet.compiler.utils.validation.ValidationError;
import io.techery.janet.http.annotations.HttpAction;
import io.techery.janet.validation.HttpActionValidators;

@AutoService(Processor.class)
public class JanetHttpProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Messager messager;
    private ClassValidator classValidator;
    private HttpActionValidators httpActionValidators;
    private HelpersFactoryGenerator helpersFactoryGenerator;
    private HttpHelpersGenerator httpHelpersGenerator;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        classValidator = new ClassValidator(HttpAction.class);
        httpActionValidators = new HttpActionValidators();
        Filer filer = processingEnv.getFiler();
        helpersFactoryGenerator = new HelpersFactoryGenerator(filer);
        httpHelpersGenerator = new HttpHelpersGenerator(filer);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotataions = new HashSet<String>();
        annotataions.add(HttpAction.class.getCanonicalName());
        return annotataions;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) return true;
        ArrayList<HttpActionClass> actionClasses = new ArrayList<HttpActionClass>();
        for (Element actionElement : roundEnv.getElementsAnnotatedWith(HttpAction.class)) {
            TypeElement typeElement = (TypeElement) actionElement;
            HttpActionClass actionClass = createActionClass(typeElement);
            if (actionClass != null) {
                Set<ValidationError> errors = httpActionValidators.validate(actionClass);
                if (!errors.isEmpty()) {
                    printErrors(errors);
                }
                actionClasses.add(actionClass);
            }
        }
        if (!actionClasses.isEmpty()) {
            httpHelpersGenerator.generate(actionClasses);
        }
        helpersFactoryGenerator.generate(actionClasses);
        return true;
    }

    private HttpActionClass createActionClass(TypeElement actionElement) {
        Set<ValidationError> errors = classValidator.validate(actionElement);
        if (!errors.isEmpty()) {
            printErrors(errors);
            return null;
        }
        HttpActionClass parent = null;
        if (actionElement.getSuperclass() != null) {
            TypeElement parentElement = elementUtils.getTypeElement(actionElement.getSuperclass()
                    .toString());
            if (parentElement != null) {
                HttpActionClass subClass = createActionClass(parentElement);
                if (subClass != null && !subClass.getAllAnnotatedMembers().isEmpty()) {
                    parent = subClass;
                }
            }
        }
        return new HttpActionClass(elementUtils, actionElement, parent);
    }

    private void printErrors(Collection<ValidationError> errors) {
        for (ValidationError error : errors) {
            messager.printMessage(Diagnostic.Kind.ERROR, error.getMessage(), error.getElement());
        }
    }

}