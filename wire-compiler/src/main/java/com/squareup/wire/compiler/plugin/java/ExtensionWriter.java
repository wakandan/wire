package com.squareup.wire.compiler.plugin.java;

import com.google.common.base.Joiner;
import com.squareup.javawriter.ClassName;
import com.squareup.javawriter.ClassWriter;
import com.squareup.javawriter.FieldWriter;
import com.squareup.javawriter.ParameterizedTypeName;
import com.squareup.javawriter.Snippet;
import com.squareup.javawriter.TypeName;
import com.squareup.protoparser.ExtendElement;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.Scalars;
import com.squareup.wire.Extension;

import static com.google.common.io.Files.getNameWithoutExtension;
import static com.squareup.wire.compiler.plugin.java.Util.getJavaPackageForProto;
import static com.squareup.wire.compiler.plugin.java.Util.isOptionType;
import static com.squareup.wire.compiler.plugin.java.Util.writeNoInstanceConstructor;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class ExtensionWriter {
  private static final String PREFIX = "Ext_";

  static ExtensionWriter forProtoFile(ProtoFile protoFile) {
    return new ExtensionWriter(protoFile);
  }

  private final ProtoFile protoFile;

  public ExtensionWriter(ProtoFile protoFile) {
    this.protoFile = protoFile;
  }

  public ClassWriter write(boolean includeOptions) {
    String simpleName = PREFIX + getNameWithoutExtension(protoFile.filePath());
    String packageName = getJavaPackageForProto(protoFile);

    ClassWriter classWriter = ClassWriter.forClassName(ClassName.create(packageName, simpleName));
    classWriter.addModifiers(PUBLIC, FINAL);
    writeNoInstanceConstructor(classWriter);

    for (ExtendElement extendDeclaration : protoFile.extendDeclarations()) {
      if (includeOptions && isOptionType(extendDeclaration.qualifiedName())) {
        // TODO put each extend declaration in separate types rather than rolled into one?
        for (FieldElement field : extendDeclaration.fields()) {
          writeExtendDeclaration(classWriter, extendDeclaration, field);
        }
      }
    }

    return classWriter;
  }

  private void writeExtendDeclaration(ClassWriter classWriter,
      ExtendElement extendDeclaration, FieldElement field) {
    TypeName extendType = ClassName.bestGuessFromString(extendDeclaration.qualifiedName());
    TypeName extensionType = ParameterizedTypeName.create(Extension.class, extendType);

    Snippet builderFactorySnippet = builderFactorySnippet(extendType, field);
    String buildMethodName = buildMethodNameFor(field);

    String initializerFormat = Joiner.on('\n').join(
        "%s",
        "    .%s",
        "    .setName(\"%s\")",
        "    .setTag(%s)",
        "    .%s()"
    );
    Snippet initializer = Snippet.format(initializerFormat, ClassName.fromClass(Extension.class),
        builderFactorySnippet, field.name(), field.tag(), buildMethodName);

    FieldWriter fieldWriter = classWriter.addField(extensionType, field.name());
    fieldWriter.addModifiers(PUBLIC, STATIC, FINAL);
    fieldWriter.setInitializer(initializer);
  }

  private Snippet builderFactorySnippet(TypeName extendType, FieldElement field) {
    switch (field.type()) {
      case Scalars.TYPE_BOOL:
        return Snippet.format("boolExtending(%s.class)", extendType);
      case Scalars.TYPE_BYTES:
        return Snippet.format("bytesExtending(%s.class)", extendType);
      case Scalars.TYPE_DOUBLE:
        return Snippet.format("doubleExtending(%s.class)", extendType);
      case Scalars.TYPE_FIXED_32:
        return Snippet.format("fixed32Extending(%s.class)", extendType);
      case Scalars.TYPE_FIXED_64:
        return Snippet.format("fixed64Extending(%s.class)", extendType);
      case Scalars.TYPE_FLOAT:
        return Snippet.format("floatExtending(%s.class)", extendType);
      case Scalars.TYPE_INT_32:
        return Snippet.format("int32Extending(%s.class)", extendType);
      case Scalars.TYPE_INT_64:
        return Snippet.format("int64Extending(%s.class)", extendType);
      case Scalars.TYPE_SFIXED_32:
        return Snippet.format("sfixed32Extending(%s.class)", extendType);
      case Scalars.TYPE_SFIXED_64:
        return Snippet.format("sfixed64Extending(%s.class)", extendType);
      case Scalars.TYPE_SINT_32:
        return Snippet.format("sint32Extending(%s.class)", extendType);
      case Scalars.TYPE_SINT_64:
        return Snippet.format("sint64Extending(%s.class)", extendType);
      case Scalars.TYPE_STRING:
        return Snippet.format("stringExtending(%s.class)", extendType);
      case Scalars.TYPE_UINT_32:
        return Snippet.format("uint32Extending(%s.class)", extendType);
      case Scalars.TYPE_UINT_64:
        return Snippet.format("uint64Extending(%s.class)", extendType);
      default:
        // TODO enum or message?
        TypeName messageName = ClassName.bestGuessFromString(field.type());
        return Snippet.format("messageExtending(%s.class, %s.class)", messageName, extendType);
    }
  }

  private String buildMethodNameFor(FieldElement field) {
    if (field.isPacked()) {
      return "buildPacked";
    }
    switch (field.label()) {
      case OPTIONAL:
        return "buildOptional";
      case REQUIRED:
        return "buildRequired";
      case REPEATED:
        return "buildRepeated";
      default:
        throw new IllegalArgumentException("Unknown field label: " + field.label());
    }
  }
}
