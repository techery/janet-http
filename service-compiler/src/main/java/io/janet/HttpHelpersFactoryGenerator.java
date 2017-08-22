package io.janet;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

import io.janet.HttpActionService.ActionHelper;
import io.janet.compiler.utils.Generator;

import static com.squareup.javapoet.MethodSpec.constructorBuilder;
import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;

public class HttpHelpersFactoryGenerator extends Generator<HttpActionClass> {

    private final List<TypeElement> otherFactoriesElements;
    private final Options options;

    public HttpHelpersFactoryGenerator(Filer filer, List<TypeElement> otherFactories, Options options) {
        super(filer);
        this.otherFactoriesElements = otherFactories;
        this.options = options;
    }

    @Override
    public void generate(ArrayList<HttpActionClass> actionClasses) {
        String className = HttpActionService.HELPERS_FACTORY_CLASS_SIMPLE_NAME;
        if (options.factoryClassSuffix != null) className += options.factoryClassSuffix;

        TypeSpec.Builder classBuilder = classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Auto-generated class factory to create action helpers for actions")
                .addSuperinterface(ParameterizedTypeName.get(HttpActionService.ActionHelperFactory.class));

        MethodSpec.Builder constructorBuilder = constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        MethodSpec.Builder makeMethodBuilder = methodBuilder("make")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ActionHelper.class)
                .addParameter(Class.class, "actionClass");

        if (shouldIncludeOtherFactories()) {
            // add field for other factories and initialize it in constructor
            ParameterizedTypeName listType = ParameterizedTypeName.get(ArrayList.class, HttpActionService.ActionHelperFactory.class);
            classBuilder.addField(
                    FieldSpec.builder(listType, "otherFactories", Modifier.PRIVATE)
                            .initializer("new $T()", listType)
                            .build()
            );
            for (TypeElement factoryElement : otherFactoriesElements) {
                constructorBuilder.addStatement("otherFactories.add(new $T())", ClassName.get(factoryElement));
            }
            // add other factories pre-check
            makeMethodBuilder
                    .beginControlFlow("for ($T factory : otherFactories)", HttpActionService.ActionHelperFactory.class)
                    .addStatement("$T helper = factory.make(actionClass)", ActionHelper.class)
                    .addStatement("if (helper != null) return helper")
                    .endControlFlow();
        }
        // factory method logic
        for (HttpActionClass actionClass : actionClasses) {
            makeMethodBuilder.beginControlFlow("if (actionClass == $T.class)", actionClass.getTypeElement());
            makeMethodBuilder.addCode(createMethodBlock(actionClass));
            makeMethodBuilder.addStatement("return helper");
            makeMethodBuilder.endControlFlow();
        }
        makeMethodBuilder.addStatement("return null");
        //
        classBuilder.addMethod(constructorBuilder.build());
        classBuilder.addMethod(makeMethodBuilder.build());
        //
        saveClass(HttpActionService.HELPERS_FACTORY_CLASS_PACKAGE, classBuilder.build());
    }

    private boolean shouldIncludeOtherFactories() {
        return !otherFactoriesElements.isEmpty() && options.factoryClassSuffix == null;
    }

    private static CodeBlock createMethodBlock(HttpActionClass actionClass) {
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
        if (actionClass.getParent() != null) {
            codeBlockBuilder.add(createMethodBlock(actionClass.getParent()));
            codeBlockBuilder.addStatement("helper = new $T(($T)helper)", actionClass.getHelperName(), actionClass.getParent()
                    .getHelperName());
        } else {
            codeBlockBuilder.addStatement("$T helper = new $T()", ActionHelper.class, actionClass.getHelperName());
        }
        return codeBlockBuilder.build();
    }
}
