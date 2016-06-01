package io.techery.janet;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;

import io.techery.janet.compiler.utils.Generator;

public class HelpersFactoryGenerator extends Generator<HttpActionClass> {

    public HelpersFactoryGenerator(Filer filer) {
        super(filer);
    }

    @Override
    public void generate(ArrayList<HttpActionClass> actionClasses) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(HttpActionService.HELPERS_FACTORY_CLASS_SIMPLE_NAME)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addJavadoc("Auto-generated class factory for initialization action helpers")
                .addSuperinterface(ParameterizedTypeName.get(HttpActionService.ActionHelperFactory.class));

        MethodSpec.Builder makeMethodBuilder = MethodSpec.methodBuilder("make")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(HttpActionService.ActionHelper.class)
                .addParameter(Class.class, "actionClass");

        for (HttpActionClass actionClass : actionClasses) {
            makeMethodBuilder.beginControlFlow("if (actionClass == $T.class)", actionClass.getTypeElement());
            makeMethodBuilder.addCode(createMethodBlock(actionClass));
            makeMethodBuilder.addStatement("return helper");
            makeMethodBuilder.endControlFlow();
        }
        makeMethodBuilder.addStatement("return null");
        classBuilder.addMethod(makeMethodBuilder.build());
        saveClass(HttpActionService.class.getPackage().getName(), classBuilder.build());
    }

    private static CodeBlock createMethodBlock(HttpActionClass actionClass) {
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder();
        if (actionClass.getParent() != null) {
            codeBlockBuilder.add(createMethodBlock(actionClass.getParent()));
            codeBlockBuilder.addStatement("helper = new $T(($T)helper)", actionClass.getHelperName(), actionClass.getParent()
                    .getHelperName());
        } else {
            codeBlockBuilder.addStatement("$T helper = new $T()", HttpActionService.ActionHelper.class, actionClass.getHelperName());
        }
        return codeBlockBuilder.build();
    }
}
