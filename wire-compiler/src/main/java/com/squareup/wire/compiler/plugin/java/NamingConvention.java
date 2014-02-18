package com.squareup.wire.compiler.plugin.java;

import java.util.Locale;

/** Naming conventions for converting from protocol buffer names to Java names. */
public interface NamingConvention {
  /**
   * Returns the Java field name for the specified protocol buffer field name. This is also used
   * for naming the parameter of the respective builder method.
   * <p>
   * Given "first_name" from:
   * <pre>
   * optional string first_name = 1;
   * </pre>
   * Generate {@code <value>} for:
   * <pre>
   * public final String &lt;value>;
   * </pre>
   */
  String getFieldName(String protoFieldName);

  /**
   * Returns the Java constant name for the specified protocol buffer field name.
   * <p>
   * Given "first_name" from:
   * <pre>
   * optional string first_name = 1;
   * </pre>
   * Generate {@code <value>} for:
   * <pre>
   * public static final String &lt;value> = "";
   * </pre>
   */
  String getDefaultConstantName(String protoFieldName);

  /**
   * Returns the Java method name for the specified protocol buffer field name.
   * <p>
   * Given "first_name" from:
   * <pre>
   * optional string first_name = 1;
   * </pre>
   * Generate {@code <value>} for:
   * <pre>
   * public Builder &lt;value>(String first_name) {
   *   // ...
   * }
   * </pre>
   */
  String getBuilderMethodName(String protoFieldName);

  /**
   * Returns the Java class name for the specified protocol buffer extend declaration name.
   */
  String getExtendDeclarationName(String protoTypeName);

  /** Default Java naming conventions. See the individual method documentation for behavior. */
  public static class Default implements NamingConvention {
    /**
     * {@inheritDoc}
     * <p>
     * Note: This implementation returns the original field name as-is.
     */
    @Override public String getFieldName(String protoFieldName) {
      return protoFieldName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: This implementation returns the original field name as-is.
     */
    @Override public String getBuilderMethodName(String protoFieldName) {
      return protoFieldName;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: This implementation returns the field name as uppercase prefixed with "DEFAULT_".
     * For example, "first_name" becomes "DEFAULT_FIRST_NAME".
     */
    @Override public String getDefaultConstantName(String protoFieldName) {
      return "DEFAULT_" + protoFieldName.toUpperCase(Locale.US);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: This implementation returns the type name prefixed with "Ext_". For example, "FooBar"
     * becomes "Ext_FooBar".
     */
    @Override public String getExtendDeclarationName(String protoTypeName) {
      return "Ext_" + protoTypeName;
    }
  }
}
