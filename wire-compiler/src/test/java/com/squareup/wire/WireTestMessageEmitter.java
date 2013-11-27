package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import java.io.IOException;
import java.util.Map;



public class WireTestMessageEmitter implements MessageEmitter {

  public static final String BUILD_EMITTER_STRING = "hello build emitter test";

  @Override public void emitBuildAdditions(JavaWriter writer, Map<String, ?> optionsMap) {
    try {
      writer.emitStatement(BUILD_EMITTER_STRING);
    } catch (IOException e)  {
    }
  }
}
