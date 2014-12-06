package com.squareup.wire.compiler.plugin.java;

import com.google.common.collect.ImmutableList;
import com.squareup.javawriter.ClassWriter;
import com.squareup.protoparser.ExtendElement;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.OptionElement;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ServiceElement;
import com.squareup.protoparser.TypeElement;
import org.junit.Test;

import static com.squareup.protoparser.MessageElement.Label.OPTIONAL;
import static com.squareup.protoparser.Scalars.TYPE_STRING;
import static org.assertj.core.api.Assertions.assertThat;

public final class ExtensionWriterTest {
  // TODO test various builder methods
  // TODO test primitives
  // TODO test message vs. enum

  @Test public void litmus() {
    FieldElement one = FieldElement.create(OPTIONAL, TYPE_STRING, "test", 1234, "",
        ImmutableList.<OptionElement>of());
    ExtendElement extendDeclaration =
        ExtendElement.create("MessageOptions", "google.protobuf.MessageOptions", "",
            ImmutableList.of(one));
    OptionElement option = OptionElement.create("java_package", "com.example", false);
    ProtoFile protoFile = ProtoFile.create("test.proto", "com.example", ImmutableList.<String>of(),
        ImmutableList.<String>of(), ImmutableList.<TypeElement>of(),
        ImmutableList.<ServiceElement>of(), ImmutableList.of(extendDeclaration),
        ImmutableList.of(option));

    ExtensionWriter extensionWriter = ExtensionWriter.forProtoFile(protoFile);
    ClassWriter javaWriter = extensionWriter.write(true);
    String actual = javaWriter.toString();

    String expected = ""
        + "";

    assertThat(actual).isEqualTo(expected);
  }
}
