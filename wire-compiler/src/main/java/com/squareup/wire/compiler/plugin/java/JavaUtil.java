package com.squareup.wire.compiler.plugin.java;

import com.squareup.javawriter.ClassWriter;
import com.squareup.javawriter.ConstructorWriter;
import com.squareup.javawriter.Snippet;

import static javax.lang.model.element.Modifier.PRIVATE;

final class JavaUtil {
  private static final Snippet NO_INSTANCES =
      Snippet.format("throw new AssertionError(\"No instances.\");");

  static void writeNoInstanceConstructor(ClassWriter classWriter) {
    ConstructorWriter constructor = classWriter.addConstructor();
    constructor.addModifiers(PRIVATE);
    constructor.body().addSnippet(NO_INSTANCES);
  }

  private JavaUtil() {
    throw new AssertionError("No instances.");
  }
}
