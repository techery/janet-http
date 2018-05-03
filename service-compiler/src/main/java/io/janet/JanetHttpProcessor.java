package io.janet;

import com.google.auto.service.AutoService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import io.janet.HttpActionService.ActionHelperFactory;
import io.janet.compiler.utils.validation.ClassValidator;
import io.janet.compiler.utils.validation.ValidationError;
import io.janet.http.annotations.HttpAction;
import io.janet.util.ElementResolver;
import io.janet.validation.HttpActionValidators;

import static io.janet.HttpActionService.HELPERS_FACTORY_CLASS_PACKAGE;
import static io.janet.HttpActionService.HELPERS_FACTORY_CLASS_SIMPLE_NAME;

/**
 * Generates {@link HttpAction} helper classes and {@link ActionHelperFactory} implementation.
 * <p>
 * Action helper is guaranteed to be generated only if it's not already in class path.
 * <p>
 * Factory takes other factories into account, that could exists in class path (e.g. via external dependency)
 * to generate canonical factory that's loaded from {@link HttpActionService} on runtime.
 * If another canonical factory exists, it's ignored and possibly would cause clash on runtime.
 * <p>
 * To generate unique factory (and could be used as dependency later), consider using annotation param {@link Options#OPTION_FACTORY_CLASS_SUFFIX}
 */
@AutoService(Processor.class)
public class JanetHttpProcessor extends AbstractProcessor {
    private Elements elementUtils;
    private Messager messager;
    private ClassValidator classValidator;
    private HttpActionValidators httpActionValidators;
    private HttpHelpersFactoryGenerator httpHelpersFactoryGenerator;
    private HttpHelpersGenerator httpHelpersGenerator;
    private Types typesUtil;
    private Options options;

    private boolean processed;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        options = new Options(processingEnv.getOptions());
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        typesUtil = processingEnv.getTypeUtils();
        ElementResolver resolver = new ElementResolver(elementUtils);
        classValidator = new ClassValidator(HttpAction.class);
        httpActionValidators = new HttpActionValidators(resolver);
        Filer filer = processingEnv.getFiler();

        httpHelpersFactoryGenerator = new HttpHelpersFactoryGenerator(filer, findOtherHelpersFactories(), options);
        httpHelpersGenerator = new HttpHelpersGenerator(filer, resolver);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override public Set<String> getSupportedOptions() {
        return Collections.singleton(Options.OPTION_FACTORY_CLASS_SUFFIX);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (processed) return false;
        else processed = true;
        //
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
        httpHelpersGenerator.generate(actionClasses);
        httpHelpersFactoryGenerator.generate(actionClasses);
        //
        return false;
    }

    private List<TypeElement> findOtherHelpersFactories() {
        PackageElement packageElement = elementUtils.getPackageElement(HELPERS_FACTORY_CLASS_PACKAGE);
        if (packageElement == null) return Collections.emptyList();
        //
        List<TypeElement> factoryElements = new ArrayList<TypeElement>();
        TypeElement interfaceElement = elementUtils.getTypeElement(ActionHelperFactory.class.getCanonicalName());
        for (Element element : packageElement.getEnclosedElements()) {
            if (element.equals(interfaceElement)
                    || element.getSimpleName().contentEquals(HELPERS_FACTORY_CLASS_SIMPLE_NAME)) continue;
            //
            for (TypeMirror typeMirror : ((TypeElement) element).getInterfaces()) {
                if (typesUtil.isAssignable(typeMirror, interfaceElement.asType())) {
                    factoryElements.add(((TypeElement) element));
                    break;
                }
            }
        }
        return factoryElements;
    }

    private HttpActionClass createActionClass(TypeElement actionElement) {
        Set<ValidationError> errors = classValidator.validate(actionElement);
        if (!errors.isEmpty()) {
            printErrors(errors);
            return null;
        }
        HttpActionClass parent = null;
        if (actionElement.getSuperclass() != null) {
            TypeElement parentElement = (TypeElement) typesUtil.asElement(actionElement.getSuperclass());
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
