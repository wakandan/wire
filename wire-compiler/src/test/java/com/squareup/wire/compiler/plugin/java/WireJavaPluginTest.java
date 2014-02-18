package com.squareup.wire.compiler.plugin.java;

import com.google.common.collect.ImmutableSet;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.Option;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.Type;
import com.squareup.wire.compiler.FileSystemRule;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static com.squareup.wire.compiler.TestUtil.NO_DEPENDENCIES;
import static com.squareup.wire.compiler.TestUtil.NO_EXTEND_DECLARATIONS;
import static com.squareup.wire.compiler.TestUtil.NO_EXTENSIONS;
import static com.squareup.wire.compiler.TestUtil.NO_FIELDS;
import static com.squareup.wire.compiler.TestUtil.NO_OPTIONS;
import static com.squareup.wire.compiler.TestUtil.NO_SERVICES;
import static com.squareup.wire.compiler.TestUtil.NO_TYPES;
import static org.fest.assertions.api.Assertions.assertThat;

public class WireJavaPluginTest {
  @Rule public FileSystemRule fileSystemRule = new FileSystemRule();

  private FileSystem fs;
  private WireJavaPlugin plugin;

  @Before public void setUp() throws IOException {
    fs = fileSystemRule.get();
    Path outputDirectory = fs.getPath("/out");
    plugin = new WireJavaPlugin().setOutputDirectory(outputDirectory);
  }

  @Test public void singleType() throws IOException {
    Option javaPackage = new Option("java_package", "com.example");
    Type message =
        new MessageType("Test", "example.Test", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile = new ProtoFile("test.proto", "example", NO_DEPENDENCIES, NO_DEPENDENCIES,
        Arrays.asList(message), NO_SERVICES, Arrays.asList(javaPackage), NO_EXTEND_DECLARATIONS);
    Set<ProtoFile> protoFiles = ImmutableSet.of(protoFile);
    plugin.generate(protoFiles);

    assertThat(Files.isRegularFile(fs.getPath("/out/com/example/Test.java"))).isTrue();
  }

  @Ignore // TODO depends on https://github.com/square/javawriter/pull/47
  @Test public void noJavaPackage() throws IOException {
    Type message =
        new MessageType("Test", "example.Test", "", NO_FIELDS, NO_TYPES, NO_EXTENSIONS, NO_OPTIONS);
    ProtoFile protoFile = new ProtoFile("test.proto", "example", NO_DEPENDENCIES, NO_DEPENDENCIES,
        Arrays.asList(message), NO_SERVICES, NO_OPTIONS, NO_EXTEND_DECLARATIONS);
    Set<ProtoFile> protoFiles = ImmutableSet.of(protoFile);
    plugin.generate(protoFiles);

    assertThat(Files.isRegularFile(fs.getPath("/out/Test.java"))).isTrue();
  }
}
