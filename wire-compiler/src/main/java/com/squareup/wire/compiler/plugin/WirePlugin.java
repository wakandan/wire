package com.squareup.wire.compiler.plugin;

import com.squareup.protoparser.ProtoFile;
import java.io.IOException;
import java.util.Set;

public interface WirePlugin {
  /**
   * TODO describe me!
   *
   * @param protoFiles The filtered types to generate.
   */
  void generate(Set<ProtoFile> protoFiles) throws IOException;
}
