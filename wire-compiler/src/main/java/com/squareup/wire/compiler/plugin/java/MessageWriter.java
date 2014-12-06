package com.squareup.wire.compiler.plugin.java;

import com.google.common.base.Joiner;
import com.squareup.javawriter.AnnotationWriter;
import com.squareup.javawriter.BlockWriter;
import com.squareup.javawriter.ClassName;
import com.squareup.javawriter.ClassWriter;
import com.squareup.javawriter.ConstructorWriter;
import com.squareup.javawriter.FieldWriter;
import com.squareup.javawriter.MethodWriter;
import com.squareup.javawriter.ParameterizedTypeName;
import com.squareup.javawriter.Snippet;
import com.squareup.javawriter.TypeName;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.Scalars;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import okio.ByteString;

import static com.squareup.protoparser.MessageElement.Label.REPEATED;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class MessageWriter {
  private static final ClassName MESSAGE_BASE_CLASS = ClassName.fromClass(Message.class);
  private static final ClassName BUILDER_BASE_CLASS = ClassName.fromClass(Message.Builder.class);

  ClassWriter write(MessageElement messageElement) {
    return write(messageElement, false);
  }

  private ClassWriter write(MessageElement messageElement, boolean nested) {
    ClassName messageClassName = ClassName.bestGuessFromString(messageElement.qualifiedName());

    // Message outer-class.
    ClassWriter messageClass = ClassWriter.forClassName(messageClassName);
    messageClass.addModifiers(PUBLIC, FINAL);
    if (nested) {
      messageClass.addModifiers(STATIC);
    }
    messageClass.setSupertype(MESSAGE_BASE_CLASS);

    // Builder inner-class.
    ClassWriter builderClass = messageClass.addNestedClass("Builder");
    builderClass.addModifiers(PUBLIC, STATIC, FINAL);
    builderClass.setSupertype(ParameterizedTypeName.create(BUILDER_BASE_CLASS, messageClassName));

    // Message public constructor.
    ConstructorWriter messagePublicConstructor = messageClass.addConstructor();
    messagePublicConstructor.addModifiers(PUBLIC);

    // Message builder constructor.
    ConstructorWriter messageBuilderConstructor = messageClass.addConstructor();
    messageBuilderConstructor.addModifiers(PRIVATE);
    messageBuilderConstructor.addParameter(builderClass.name(), "builder");
    List<Snippet> builderReferences = new ArrayList<>();

    // Message equals() method.
    MethodWriter messageEquals = messageClass.addMethod(boolean.class, "equals");
    messageEquals.addModifiers(PUBLIC);
    messageEquals.annotate(Override.class);
    messageEquals.body().addSnippet(Joiner.on('\n').join(
        "if (other == this) return true;",
        "if (!(other instanceof %1$s)) return false;",
        "%1$s o = (%1$s) other;"
        ), messageClassName);
    // TODO extensions equals?
    List<Snippet> equalsSnippets = new ArrayList<>();

    // Message hashCode() method.
    MethodWriter messageHashCode = messageClass.addMethod(int.class, "hashCode");
    messageHashCode.addModifiers(PUBLIC);
    messageHashCode.annotate(Override.class);
    BlockWriter messageHashCodeBody = messageHashCode.body();
    messageHashCodeBody.addSnippet(Joiner.on('\n').join(
        "int result = hashCode;",
        "if (result != 0) return result;"
    ));
    // TODO extensions hashCode?

    // Builder default constructor.
    builderClass.addConstructor().addModifiers(PUBLIC);

    // Builder factory constructor.
    ConstructorWriter builderConstructor = builderClass.addConstructor();
    builderConstructor.addModifiers(PUBLIC);
    builderConstructor.addParameter(messageClassName, "message");
    BlockWriter builderConstructorBody = builderConstructor.body();
    builderConstructorBody.addSnippet("super(message);");
    builderConstructorBody.addSnippet("if (message == null) return;");

    // Builder build() method.
    MethodWriter builderBuild = builderClass.addMethod(messageClassName, "build");
    builderBuild.addModifiers(PUBLIC);
    builderBuild.annotate(Override.class);
    builderBuild.body().addSnippet("return new %s(this);", messageClassName);

    for (FieldElement field : messageElement.fields()) {
      String name = field.name();
      boolean isList = field.label() == REPEATED || field.isPacked();
      TypeName typeName = null; // TODO resolve types!
      boolean isMessageType = false; // TODO resolve message vs. enum.

      if (!isMessageType) {
        FieldWriter defaultField = messageClass.addField(typeName, name.toUpperCase(Locale.US));
        defaultField.addModifiers(PUBLIC, STATIC, FINAL);
        defaultField.setInitializer(defaultInitializer(isList, field.type()));
      }

      FieldWriter messageField = messageClass.addField(typeName, name);
      messageField.addModifiers(PUBLIC, FINAL);
      AnnotationWriter messageFieldAnnotation = messageField.annotate(ProtoField.class);
      messageFieldAnnotation.setMember("tag", field.tag());
      // TODO messageFieldAnnotation.setMember("typeName", );

      messagePublicConstructor.addParameter(typeName, name);
      if (isList) {
        messagePublicConstructor.body().addSnippet("this.%1$s = immutableCopyOf(%1$s)", name);
      } else {
        messagePublicConstructor.body().addSnippet("this.%1$s = %1$s", name);
      }

      builderReferences.add(Snippet.format("builder.%s", name));

      equalsSnippets.add(Snippet.format("equals(%$1s, o.%$1s)", name));

      messageHashCodeBody.addSnippet(
          "result = result * 37 + (%1$s != null ? %1$s.hashCode() : %2$s);", name, isList ? 1 : 0);

      builderClass.addField(typeName, name).addModifiers(PUBLIC);

      if (isList) {
        builderConstructorBody.addSnippet("this.%1$s = copyOf(message.%1$s;)", name);
      } else {
        builderConstructorBody.addSnippet("this.%1$s = message.%1$s;", name);
      }

      MethodWriter builderMethod = builderClass.addMethod(builderClass.name(), name);
      builderMethod.addModifiers(PUBLIC);
      builderMethod.addParameter(typeName, name);
      builderMethod.body().addSnippet(Joiner.on('\n').join("this.%1$s = %1$s;", "return this;"),
          name);
    }

    // Finish the builder constructor.
    BlockWriter messageBuilderConstructorBody = messageBuilderConstructor.body();
    messageBuilderConstructorBody.addSnippet("this(%s);", Joiner.on(", ").join(builderReferences));
    messageBuilderConstructorBody.addSnippet("setBuilder(builder)");

    // Finish the equals() method.
    messageEquals.body().addSnippet("return %s;", Joiner.on("\n    && ").join(equalsSnippets));

    return messageClass;
  }

  private static Snippet defaultInitializer(boolean isList, String type) {
    // TODO support regular default values.

    if (isList) {
      return Snippet.format("%s.emptyList();", Collections.class);
    }
    switch (type) {
      case Scalars.TYPE_BOOL:
        return Snippet.format("false;");
      case Scalars.TYPE_BYTES:
        return Snippet.format("%s.EMPTY;", ByteString.class);
      case Scalars.TYPE_DOUBLE:
        return Snippet.format("0d;");
      case Scalars.TYPE_FIXED_32:
      case Scalars.TYPE_INT_32:
      case Scalars.TYPE_SFIXED_32:
      case Scalars.TYPE_SINT_32:
      case Scalars.TYPE_UINT_32:
        return Snippet.format("0;");
      case Scalars.TYPE_FIXED_64:
      case Scalars.TYPE_INT_64:
      case Scalars.TYPE_SFIXED_64:
      case Scalars.TYPE_SINT_64:
      case Scalars.TYPE_UINT_64:
        return Snippet.format("0L;");
      case Scalars.TYPE_FLOAT:
        return Snippet.format("0f;");
      case Scalars.TYPE_STRING:
        return Snippet.format("\"\";");
      default:
        // TODO get first enum value and return.
        return Snippet.format("null; // TODO");
    }
  }
}
