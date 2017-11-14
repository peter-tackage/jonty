/*
 * Copyright 2017 Peter Tackage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.petertackage.jonty.processor;

import com.google.auto.service.AutoService;
import com.petertackage.jonty.Fieldable;
import com.squareup.kotlinpoet.ClassName;
import com.squareup.kotlinpoet.FileSpec;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@AutoService(Processor.class)
public final class JontyProcessor extends AbstractProcessor {

    private static final String OPTION_DEBUGGABLE = "jonty.debuggable";

    private Elements elementUtils;
    private Filer filer;
    private boolean debuggable = true;

    public JontyProcessor() {
        super();
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        elementUtils = env.getElementUtils();
        filer = env.getFiler();
        debuggable = !"false".equals(env.getOptions().get(OPTION_DEBUGGABLE));
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment roundEnv) {

        note(null, "Starting processings");

        Map<TypeElement, Fielder> fielderMap = findAndParseFields(roundEnv);

        for (Map.Entry<TypeElement, Fielder> entry : fielderMap.entrySet()) {
            TypeElement typeElement = entry.getKey();
            Fielder fielder = entry.getValue();

            // Write it out to a file.
            FileSpec kotlinFile = fielder.brew(debuggable);

            try {
                kotlinFile.writeTo(new File("/Users/ptac/gen"));
            } catch (IOException e) {
                error(typeElement, "Unable to write fielder for type %s: %s", typeElement, e.getMessage());
            }

        }

        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Fieldable.class.getCanonicalName());
    }

    private Map<TypeElement, Fielder> findAndParseFields(RoundEnvironment env) {

        // Maps of classes to builders
        Map<TypeElement, Fielder.Builder> builderMap = new LinkedHashMap<>();

        // Process each @Fieldable class element.
        for (Element element : env.getElementsAnnotatedWith(Fieldable.class)) {
            note(element, "Got this element %s", element);
            try {
                parseFieldableClass(element, builderMap);
            } catch (Exception e) {
                logParsingError(element, Fieldable.class, e);
            }
        }

        Map<TypeElement, Fielder> fielderMap = new LinkedHashMap<>();

        for (Map.Entry<TypeElement, Fielder.Builder> entry : builderMap.entrySet()) {
            fielderMap.put(entry.getKey(), entry.getValue().build());
        }
        return fielderMap;
    }

    private void parseFieldableClass(Element element,
                                     Map<TypeElement, Fielder.Builder> builderMap) {

        TypeElement typeElement = (TypeElement) element;
        String packageName = getPackageName(typeElement);
        String className = getClassName(typeElement, packageName);
        ClassName bindingClassName = new ClassName(packageName, className + "_JontyFielder");

        Fielder.Builder fielderBuilder = new Fielder.Builder(bindingClassName);

        builderMap.put((TypeElement) element, fielderBuilder);

        while (typeElement != null) {
            for (Element enclosedElement : typeElement.getEnclosedElements()) {
                // Only interested in fields
                if (enclosedElement.getKind() == ElementKind.FIELD) {
                    note(enclosedElement, "Element enclosed fields: %s", enclosedElement);
                    String fieldName = enclosedElement.getSimpleName().toString();
                    note(enclosedElement, "Adding field: %s", fieldName);
                    fielderBuilder.addName(fieldName);
                }
            }
            typeElement = findParentType(typeElement);
        }

    }

    private TypeElement findParentType(TypeElement typeElement) {
        note(typeElement, "Finding superclass for %s: ", typeElement);
        TypeMirror type;
        type = typeElement.getSuperclass();
        if (type.getKind() == TypeKind.NONE) {
            note(typeElement, "No superclass for: %s ", type);
            return null;
        }
        note(typeElement, "Found superclass: %s ", type);
        return (TypeElement) ((DeclaredType) type).asElement();
    }

    private static String getClassName(TypeElement type, String packageName) {
        return packageName.length() == 0 ?
                type.getQualifiedName().toString() :
                type.getQualifiedName().toString().substring(packageName.length() + 1)
                        .replace('.', '_');
    }

    private String getPackageName(TypeElement type) {
        return elementUtils.getPackageOf(type).getQualifiedName().toString();
    }

    private void logParsingError(Element element, Class<? extends Annotation> annotation,
                                 Exception e) {
        StringWriter stackTrace = new StringWriter();
        e.printStackTrace(new PrintWriter(stackTrace));
        error(element, "Unable to parse @%s fielder.\n\n%s", annotation.getSimpleName(),
                stackTrace);
    }

    private void error(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.ERROR, element, message, args);
    }

    private void note(Element element, String message, Object... args) {
        printMessage(Diagnostic.Kind.NOTE, element, message, args);
    }

    private void printMessage(Diagnostic.Kind kind, Element element, String message,
                              Object[] args) {
        if (args.length > 0) {
            message = String.format(message, args);
        }

        processingEnv.getMessager().printMessage(kind, message, element);
    }

}

