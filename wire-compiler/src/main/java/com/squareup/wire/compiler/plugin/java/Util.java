package com.squareup.wire.compiler.plugin.java;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.Option;
import com.squareup.protoparser.ProtoFile;
import com.squareup.wire.ByteString;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.protoparser.ScalarTypes.TYPE_BOOL;
import static com.squareup.protoparser.ScalarTypes.TYPE_BYTES;
import static com.squareup.protoparser.ScalarTypes.TYPE_DOUBLE;
import static com.squareup.protoparser.ScalarTypes.TYPE_FIXED_32;
import static com.squareup.protoparser.ScalarTypes.TYPE_FIXED_64;
import static com.squareup.protoparser.ScalarTypes.TYPE_FLOAT;
import static com.squareup.protoparser.ScalarTypes.TYPE_INT_32;
import static com.squareup.protoparser.ScalarTypes.TYPE_INT_64;
import static com.squareup.protoparser.ScalarTypes.TYPE_SFIXED_32;
import static com.squareup.protoparser.ScalarTypes.TYPE_SFIXED_64;
import static com.squareup.protoparser.ScalarTypes.TYPE_SINT_32;
import static com.squareup.protoparser.ScalarTypes.TYPE_SINT_64;
import static com.squareup.protoparser.ScalarTypes.TYPE_STRING;
import static com.squareup.protoparser.ScalarTypes.TYPE_UINT_32;
import static com.squareup.protoparser.ScalarTypes.TYPE_UINT_64;
import static com.squareup.protoparser.ScalarTypes.isScalarType;
import static javax.lang.model.element.Modifier.PRIVATE;

final class Util {
  private static final String URL_MATCHER = "(http:[-!#$%&'()*+,./0-9:;=?@A-Z\\[\\]_a-z~]+)";
  private static final Set<String> JAVA_KEYWORDS =
      ImmutableSet.of("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
          "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
          "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
          "int", "interface", "long", "native", "new", "package", "private", "protected", "public",
          "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
          "throw", "throws", "transient", "try", "void", "volatile", "while");
  private static final Map<String, Class<?>> JAVA_TYPES = ImmutableMap.<String, Class<?>>builder()
      .put(TYPE_BOOL, Boolean.class)
      .put(TYPE_BYTES, ByteString.class)
      .put(TYPE_DOUBLE, Double.class)
      .put(TYPE_FLOAT, Float.class)
      .put(TYPE_FIXED_32, Integer.class)
      .put(TYPE_FIXED_64, Long.class)
      .put(TYPE_INT_32, Integer.class)
      .put(TYPE_INT_64, Long.class)
      .put(TYPE_SFIXED_32, Integer.class)
      .put(TYPE_SFIXED_64, Long.class)
      .put(TYPE_SINT_32, Integer.class)
      .put(TYPE_SINT_64, Long.class)
      .put(TYPE_STRING, String.class)
      .put(TYPE_UINT_32, Integer.class)
      .put(TYPE_UINT_64, Long.class)
      .build();

  /**
   * Returns the Java type associated with a standard .proto scalar type, e.g., {@code int32},
   * {@code string}, etc., or null if the name is not that of a scalar type.
   */
  public static Class<?> getScalarType(String type) {
    checkArgument(isScalarType(type), "\"%s\" is not a scalar type.", type);
    return JAVA_TYPES.get(type);
  }

  /** A grab-bag of fixes for things that can go wrong when converting to javadoc. */
  static String sanitizeJavadoc(String documentation) {
    // JavaWriter will pass the doc through String.format, so escape all '%' chars
    documentation = documentation.replace("%", "%%");
    // Remove trailing whitespace on each line.
    documentation = documentation.replaceAll("[^\\S\n]+\n", "\n");
    documentation = documentation.replaceAll("\\s+$", "");
    // Rewrite URLs to use an html anchor tag.
    documentation = documentation.replaceAll(URL_MATCHER, "<a href=\"$1\">$1</a>");
    return documentation;
  }

  /** Prefix a name if it collides with a language reserved keyword. */
  public static String sanitizeName(String name) {
    return JAVA_KEYWORDS.contains(name) ? "_" + name : name;
  }

  /** Return a {@link File} for the fully-qualified class name in the specified directory. */
  static File getJavaFile(File directory, String className) {
    checkNotNull(directory, "Directory must not be null.");
    checkNotNull(className, "Class name must not be null.");
    return new File(directory, className.replace('.', File.separatorChar) + ".java");
  }

  /** Emit a private, no-args constructor for uninstantiable classes. */
  static void emitNoInstanceConstructor(JavaWriter javaWriter) throws IOException {
    javaWriter.beginConstructor(EnumSet.of(PRIVATE));
    javaWriter.emitStatement("throw new AssertionError(\"No instances.\")");
    javaWriter.endConstructor();
  }

  static boolean isOptionType(String type) {
    return "google.protobuf.FieldOptions".equals(type) //
        || "google.protobuf.MessageOptions".equals(type);
  }

  static String getJavaPackageForProto(ProtoFile protoFile) {
    String javaPackage =
        (String) Option.findByName(protoFile.getOptions(), "java_package").getValue();
    checkNotNull(javaPackage, "'java_package' option missing from %s", protoFile.getFileName());
    return javaPackage;
  }

  private Util() {
    throw new AssertionError("No instances.");
  }
}
