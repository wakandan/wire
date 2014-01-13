package com.squareup.wire.compiler.plugin.java;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.squareup.wire.compiler.plugin.java.WireJavaPlugin.FILE_HEADER;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class ExtensionRegistryWriter {
  private final List<String> extensions;
  private final String javaPackage;
  private final String className;

  ExtensionRegistryWriter(Set<String> extensions, String registryClass) {
    // Both a defensive copy and switching to an indexed collection for later iteration.
    this.extensions = ImmutableList.copyOf(extensions);

    int packageClassSep = registryClass.lastIndexOf('.');
    javaPackage = registryClass.substring(0, packageClassSep);
    className = registryClass.substring(packageClassSep + 1);
  }

  void write(Writer writer) throws IOException {
    final JavaWriter javaWriter = new JavaWriter(writer);

    try {
      javaWriter.emitSingleLineComment(FILE_HEADER);
      javaWriter.emitPackage(javaPackage);

      javaWriter.emitImports("java.util.List");
      javaWriter.emitImports(extensions); // Import all extension classes.
      javaWriter.emitEmptyLine();
      javaWriter.emitStaticImports("java.util.Arrays.asList");
      javaWriter.emitStaticImports("java.util.Collections.unmodifiableList");
      javaWriter.emitEmptyLine();

      javaWriter.beginType(className, "class", EnumSet.of(PUBLIC, FINAL));

      javaWriter.emitAnnotation("SuppressWarnings(\"unchecked\")");
      javaWriter.emitField("List<Class<?>>", "EXTENSIONS", EnumSet.of(PUBLIC, STATIC, FINAL),
          "unmodifiableList(asList(\n" + Joiner.on(",\n")
              .join(Lists.transform(extensions, new Function<String, String>() {
                @Override public String apply(String input) {
                  return javaWriter.compressType(input) + ".class";
                }
              })) + "))");
      javaWriter.emitEmptyLine();

      // Private, no-args constructor
      Util.emitNoInstanceConstructor(javaWriter);

      javaWriter.endType();
    } finally {
      javaWriter.close();
    }
  }
}
