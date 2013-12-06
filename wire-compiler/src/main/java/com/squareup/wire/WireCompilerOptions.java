package com.squareup.wire;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public final class WireCompilerOptions {
  public static final String PROTO_PATH_FLAG = "--proto_path=";
  public static final String JAVA_OUT_FLAG = "--java_out=";
  public static final String FILES_FLAG = "--files=";
  public static final String REGISTRY_CLASS_FLAG = "--registry_class=";
  public static final String ROOTS_FLAG = "--roots=";
  public static final String NO_OPTIONS_FLAG = "--no_options";
  public static final String EMITTERS_FLAG = "--emitters=";

  public final String protoPath;
  public final List<String> sourceFileNames;
  public final java.util.List<String> roots;
  public final String outputDirectory;
  public final String registryClass;
  public final boolean emitOptions;
  public final List<String> emitterNames;
  public final IO io;

  private WireCompilerOptions(Builder builder) {
    protoPath = builder.protoPath;
    sourceFileNames = builder.sourceFileNames;
    roots = builder.roots;
    outputDirectory = builder.outputDirectory;
    registryClass = builder.registryClass;
    emitOptions = builder.emitOptions;
    emitterNames = builder.emitterNames;
    io = builder.io;
  }

  public static final class Builder {
    public String protoPath;
    public List<String> sourceFileNames;
    public java.util.List<String> roots;
    public String outputDirectory;
    public String registryClass;
    public boolean emitOptions;
    public List<String> emitterNames;
    public IO io;

    public Builder() {
      sourceFileNames = new ArrayList<String>();
      roots = new ArrayList<String>();
      emitterNames = new ArrayList<String>();

      // Options are on by default
      emitOptions = true;
    }

    public WireCompilerOptions build() {
      if (this.outputDirectory == null) {
        throw new IllegalArgumentException("Must specify " + JAVA_OUT_FLAG + " flag");
      }
      return new WireCompilerOptions(this);
    }

    public Builder setProtoPath(String protoPath) {
      this.protoPath = protoPath;
      return this;
    }

    public Builder setSourceFileNames(List<String> sourceFileNames) {
      this.sourceFileNames = sourceFileNames;
      return this;
    }

    public Builder setRoots(List<String> roots) {
      this.roots = roots;
      return this;
    }

    public Builder setOutputDirectory(String outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    public Builder setRegistryClass(String registryClass) {
      this.registryClass = registryClass;
      return this;
    }

    public Builder setEmitOptions(boolean emitOptions) {
      this.emitOptions = emitOptions;
      return this;
    }

    public Builder setEmitterNames(List<String> emitterNames) {
      this.emitterNames = emitterNames;
      return this;
    }

    public Builder setIo(IO io) {
      this.io = io;
      return this;
    }
  }

  public static WireCompilerOptions.Builder builderFromArgs(String... args)
      throws FileNotFoundException {
    WireCompilerOptions.Builder builder = new WireCompilerOptions.Builder();
    int index = 0;
    while (index < args.length) {
      if (args[index].startsWith(PROTO_PATH_FLAG)) {
        builder.protoPath = args[index].substring(PROTO_PATH_FLAG.length());
      } else if (args[index].startsWith(JAVA_OUT_FLAG)) {
        builder.outputDirectory = args[index].substring(JAVA_OUT_FLAG.length());
      } else if (args[index].startsWith(FILES_FLAG)) {
        File files = new File(args[index].substring(FILES_FLAG.length()));
        String[] fileNames = new Scanner(files, "UTF-8").useDelimiter("\\A").next().split("\n");
        builder.sourceFileNames.addAll(Arrays.asList(fileNames));
      } else if (args[index].startsWith(ROOTS_FLAG)) {
        builder.roots.addAll(Arrays.asList(args[index].substring(ROOTS_FLAG.length()).split(",")));
      } else if (args[index].startsWith(REGISTRY_CLASS_FLAG)) {
        builder.registryClass = args[index].substring(REGISTRY_CLASS_FLAG.length());
      } else if (args[index].equals(NO_OPTIONS_FLAG)) {
        builder.emitOptions = false;
      } else if (args[index].startsWith(EMITTERS_FLAG)) {
        builder.emitterNames.addAll(
            Arrays.asList(args[index].substring(EMITTERS_FLAG.length()).split(",")));
      } else {
        builder.sourceFileNames.add(args[index]);
      }
      index++;
    }

    return builder;
  }
}
