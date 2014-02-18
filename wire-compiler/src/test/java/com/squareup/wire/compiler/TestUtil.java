package com.squareup.wire.compiler;

import com.squareup.protoparser.ExtendDeclaration;
import com.squareup.protoparser.Extensions;
import com.squareup.protoparser.Option;
import com.squareup.protoparser.Service;
import com.squareup.protoparser.Type;
import java.util.Collections;
import java.util.List;

import static com.squareup.protoparser.MessageType.Field;

public final class TestUtil {
  public static final List<Type> NO_TYPES = Collections.emptyList();
  public static final List<Extensions> NO_EXTENSIONS = Collections.emptyList();
  public static final List<Option> NO_OPTIONS = Collections.emptyList();
  public static final List<String> NO_DEPENDENCIES = Collections.emptyList();
  public static final List<Service> NO_SERVICES = Collections.emptyList();
  public static final List<ExtendDeclaration> NO_EXTEND_DECLARATIONS = Collections.emptyList();
  public static final List<Field> NO_FIELDS = Collections.emptyList();

  private TestUtil() {
    throw new AssertionError("No instances.");
  }
}
