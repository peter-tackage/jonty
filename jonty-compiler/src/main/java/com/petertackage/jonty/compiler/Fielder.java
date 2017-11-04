/*
 * Copyright 2017 Futurice GmbH
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

package com.petertackage.jonty.compiler;

import com.squareup.javapoet.*;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import static javax.lang.model.element.Modifier.*;

// TODO This should generate Kotlin code.
final class Fielder {

    private final ClassName fielderClassName;
    private final Set<String> names;

    private Fielder(ClassName fielderClassName,
                    Set<String> names) {
        this.fielderClassName = fielderClassName;
        this.names = Collections.unmodifiableSet(names);
    }

    JavaFile brewJava(boolean debuggable) {
        return JavaFile.builder(fielderClassName.packageName(), createType(debuggable))
                .addFileComment("Generated code from Jonty. Do not modify!")
                .build();
    }

    private TypeSpec createType(boolean debuggable) {
        TypeSpec.Builder result = TypeSpec.classBuilder(fielderClassName.simpleName())
                .addModifiers(PUBLIC)
                .addModifiers(FINAL)
                .addField(defineField())
                .addStaticBlock(defineFieldsBlock())
                .addMethod(createFieldsMethod());

        // TODO In Kotlin we could have this lazily loaded rather than statically defined.


        return result.build();
    }

    private FieldSpec defineField() {
        // public static final Set<String> fields;
        // TODO define
        return null;
    }

    @NotNull
    private CodeBlock defineFieldsBlock() {
        // static { ... }
        // TODO define
        return null;
    }

    private MethodSpec createFieldsMethod() {
        // TODO Should this be immutable? Probably.
        return MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addModifiers(STATIC)
                .returns(String[].class)
                .addStatement(("return fields;"))
                .build();
    }

    final static class Builder {

        private final ClassName fielderClassName;
        private final Set<String> names = new TreeSet<>();

        Builder(ClassName fielderClassName) {
            this.fielderClassName = fielderClassName;
        }

        Builder addName(String name) {
            this.names.add(name);
            return this;
        }

        Fielder build() {
            return new Fielder(fielderClassName, names);
        }
    }
}
