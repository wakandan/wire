package com.squareup.wire.compiler.plugin.java;

import com.squareup.javawriter.ClassName;
import com.squareup.javawriter.ConstructorWriter;
import com.squareup.javawriter.EnumWriter;
import com.squareup.javawriter.FieldWriter;
import com.squareup.javawriter.MethodWriter;
import com.squareup.javawriter.Snippet;
import com.squareup.protoparser.EnumConstantElement;
import com.squareup.protoparser.EnumElement;
import com.squareup.wire.ProtoEnum;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class JavaEnumWriter {
  private static final ClassName ENUM_BASE_INTERFACE = ClassName.fromClass(ProtoEnum.class);

  public EnumWriter write(EnumElement enumElement) {
    return write(enumElement, false);
  }

  EnumWriter write(EnumElement enumElement, boolean nested) {
    ClassName enumClassName = ClassName.bestGuessFromString(enumElement.qualifiedName());

    // Enum class.
    EnumWriter enumClass = EnumWriter.forClassName(enumClassName);
    enumClass.addModifiers(PUBLIC, FINAL);
    if (nested) {
      enumClass.addModifiers(STATIC);
    }
    enumClass.addImplementedType(ENUM_BASE_INTERFACE);

    FieldWriter valueField = enumClass.addField(int.class, "value");
    valueField.addModifiers(PRIVATE, FINAL);

    ConstructorWriter constructor = enumClass.addConstructor();
    constructor.addModifiers(PRIVATE);
    constructor.addParameter(int.class, "value");
    constructor.body().addSnippet("this.value = value;");

    MethodWriter valueMethod = enumClass.addMethod(int.class, "value");
    valueMethod.addModifiers(PUBLIC);
    valueMethod.annotate(Override.class);
    valueMethod.body().addSnippet("return value;");

    for (EnumConstantElement constant : enumElement.constants()) {
      enumClass.addConstant(constant.name())
          .addArgument(Snippet.format(String.valueOf(constant.tag())));

    }

    return enumClass;
  }
}
