package com.squareup.wire.compiler;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.Writer;

/** A simple abstraction of a filesystem to facilitate testing. */
public interface Filesystem {
  /** Equivalent to {@link File#exists()}. */
  boolean exists(File file);

  /** Equivalent to {@link File#isFile()}. */
  boolean isFile(File file);

  /** Equivalent to {@link File#isDirectory()}. */
  boolean isDirectory(File file);

  /** Equivalent to {@link File#listFiles()}. */
  File[] listFiles(File file);

  /** Read the entire contents of a file as UTF-8 to a String. */
  String contentsUtf8(File file) throws IOException;

  /** Create a content writer for the supplied file. */
  Writer writerUtf8(File file) throws IOException;

  /** A {@link Filesystem} which uses normal storage. */
  Filesystem SYSTEM = new Filesystem() {
    @Override public boolean exists(File file) {
      return file.exists();
    }

    @Override public boolean isFile(File file) {
      return file.isFile();
    }

    @Override public boolean isDirectory(File file) {
      return file.isDirectory();
    }

    @Override public File[] listFiles(File file) {
      return file.listFiles();
    }

    @Override public String contentsUtf8(File file) throws IOException {
      return Files.toString(file, Charsets.UTF_8);
    }

    @Override public Writer writerUtf8(File file) throws IOException {
      return Files.newWriter(file, Charsets.UTF_8);
    }
  };
}
