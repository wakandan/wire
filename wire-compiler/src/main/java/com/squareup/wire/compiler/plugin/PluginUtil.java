package com.squareup.wire.compiler.plugin;

public final class PluginUtil {
  /** Works for messages or Java classes. */
  public static boolean isOptionType(String fullyQualifiedName) {
    return isMessageOptions(fullyQualifiedName) || isFieldOptions(fullyQualifiedName);
  }

  /** Works for messages or Java classes. */
  public static boolean isFieldOptions(String name) {
    return "google.protobuf.FieldOptions".equals(name)
        || "com.google.protobuf.FieldOptions".equals(name);
  }

  /** Works for messages or Java classes. */
  public static boolean isMessageOptions(String fullyQualifiedName) {
    return "google.protobuf.MessageOptions".equals(fullyQualifiedName)
        || "com.google.protobuf.MessageOptions".equals(fullyQualifiedName);
  }

  private PluginUtil() {
    throw new AssertionError("No instances.");
  }
}
