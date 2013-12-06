package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Allows visiting / hooking into various phases of message emitting.
 *
 * Implementations should provide a no-arg constructor.
 */
public interface MessageEmitter {

  /**
   * Called in the scope of import statements.
   *
   * @param writer
   * @param options
   * @throws IOException
   */
  void emitImports(JavaWriter writer, Map<String, ?> options) throws IOException;

  /**
   * Called within the {@code build()} method before emitting the code returning the new instance.
   *
   * @param writer
   * @param options the message options.
   */
  void emitBuildAdditions(JavaWriter writer, Map<String, ?> options) throws IOException;
}
