package com.squareup.wire.compiler.plugin.java;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.ExtendDeclaration;
import com.squareup.protoparser.ProtoFile;
import java.io.IOException;
import java.io.Writer;
import java.util.EnumSet;

import static com.squareup.wire.compiler.plugin.java.Util.emitNoInstanceConstructor;
import static com.squareup.wire.compiler.plugin.java.Util.getJavaPackageForProto;
import static com.squareup.wire.compiler.plugin.java.WireJavaPlugin.FILE_HEADER;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

final class ExtensionWriter {
  private final ProtoFile protoFile;
  private final boolean emitOptions;

  ExtensionWriter(ProtoFile protoFile, boolean emitOptions) {
    this.protoFile = protoFile;
    this.emitOptions = emitOptions;
  }

  void write(Writer writer) throws IOException {
    final JavaWriter javaWriter = new JavaWriter(writer);

    try {
      javaWriter.emitSingleLineComment(FILE_HEADER);
      javaWriter.emitPackage(getJavaPackageForProto(protoFile));

      String className = "Ext_" + protoFile.getFileName();
      javaWriter.beginType(className, "class", EnumSet.of(PUBLIC, FINAL));

      for (ExtendDeclaration extend : protoFile.getExtendDeclarations()) {
        String fullyQualifiedName = extend.getFullyQualifiedName();
        if (emitOptions && !Util.isOptionType(fullyQualifiedName)) {
          writeExtendDeclaration(javaWriter, extend);
        }
      }

      emitNoInstanceConstructor(javaWriter);
      javaWriter.endType();
    } catch (IOException e) {
      javaWriter.close();
    }
  }

  private static void writeExtendDeclaration(JavaWriter javaWriter, ExtendDeclaration extend) {

  }
}
