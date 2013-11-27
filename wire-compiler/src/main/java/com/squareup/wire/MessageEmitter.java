package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import java.util.Map;

/**
 * Allows visiting / hooking into various phases of message emitting.
 *
 * Implementations should provide a no-arg constructor.
 */
public interface MessageEmitter {

  /**
   * Called within the {@code build()} method before emitting the code returning the new instance.
   *
   * @param writer
   * @param optionsMap the message options.
   */
  void emitBuildAdditions(JavaWriter writer, Map<String, ?> optionsMap);
}
