package com.squareup.wire.compiler.plugin.java;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.Option;
import com.squareup.protoparser.ProtoFile;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import javax.lang.model.element.Modifier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class JavaUtil {
  static final EnumSet<Modifier> PUBLIC_STATIC_FINAL = EnumSet.of(PUBLIC, STATIC, FINAL);
  static final EnumSet<Modifier> PUBLIC_FINAL = EnumSet.of(PUBLIC, FINAL);
  static final EnumSet<Modifier> PRIVATE = EnumSet.of(Modifier.PRIVATE);

  /** Return declared Java package name or null. */
  static String getPackageName(ProtoFile protoFile) {
    Option javaPackage = Option.findByName(protoFile.getOptions(), "java_package");
    return javaPackage != null ? (String) javaPackage.getValue() : null;
  }

  static JavaWriter newJavaWriter(Path outputDirectory, String packageName, String type)
      throws IOException {
    String relativePath = type + ".java";
    if (packageName != null) {
      String separator = outputDirectory.getFileSystem().getSeparator();
      relativePath = packageName.replace(".", separator) + separator + relativePath;
    }
    Path outputFile = outputDirectory.resolve(relativePath);
    Files.createDirectories(outputFile.getParent());

    // TODO Work around: https://github.com/marschall/memoryfilesystem/issues/9
    Files.createFile(outputFile);

    BufferedWriter writer = Files.newBufferedWriter(outputFile, UTF_8);
    return new JavaWriter(writer);
  }

  static void emitNoInstanceConstructor(JavaWriter writer) throws IOException {
    writer.beginConstructor(PRIVATE);
    writer.emitStatement("throw new AssertionError(\"No instances.\")");
    writer.endConstructor();
  }

  private JavaUtil() {
    throw new AssertionError("No instances.");
  }
}
