package com.squareup.wire.compiler.plugin.java;

import com.squareup.javawriter.ClassWriter;
import com.squareup.javawriter.ConstructorWriter;
import com.squareup.javawriter.Snippet;
import com.squareup.protoparser.OptionElement;
import com.squareup.protoparser.ProtoFile;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static javax.lang.model.element.Modifier.PRIVATE;

final class Util {
  private static final Snippet NO_INSTANCES =
      Snippet.format("throw new AssertionError(\"No instances.\");");

  static void writeNoInstanceConstructor(ClassWriter classWriter) {
    ConstructorWriter constructor = classWriter.addConstructor();
    constructor.addModifiers(PRIVATE);
    constructor.body().addSnippet(NO_INSTANCES);
  }

  static String getJavaPackageForProto(ProtoFile protoFile) {
    OptionElement packageOption = OptionElement.findByName(protoFile.options(), "java_package");
    checkNotNull(packageOption, "'java_package' option missing from %s.", protoFile.filePath());
    Object javaPackage = packageOption.value();
    checkState(javaPackage instanceof String, "'java_package' option was not a string.");
    return String.valueOf(javaPackage);
  }

  static boolean isOptionType(String type) {
    return "google.protobuf.FieldOptions".equals(type) //
        || "google.protobuf.MessageOptions".equals(type);
  }

  private Util() {
    throw new AssertionError("No instances.");
  }
}
