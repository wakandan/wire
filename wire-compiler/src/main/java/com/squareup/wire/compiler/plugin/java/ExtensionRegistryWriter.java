package com.squareup.wire.compiler.plugin.java;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.squareup.javawriter.ClassName;
import com.squareup.javawriter.ClassWriter;
import com.squareup.javawriter.FieldWriter;
import com.squareup.javawriter.JavaWriter;
import com.squareup.javawriter.ParameterizedTypeName;
import com.squareup.javawriter.Snippet;
import com.squareup.javawriter.TypeName;
import com.squareup.javawriter.TypeNames;
import com.squareup.javawriter.WildcardName;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.squareup.wire.compiler.plugin.java.JavaUtil.writeNoInstanceConstructor;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

final class ExtensionRegistryWriter {
  static ExtensionRegistryWriter forClassName(String registryName) {
    ClassName className = ClassName.bestGuessFromString(registryName);
    return new ExtensionRegistryWriter(className);
  }

  private final ClassName className;

  private ExtensionRegistryWriter(ClassName className) {
    this.className = className;
  }

  public JavaWriter writeExtensions(Set<String> extensions) {
    JavaWriter javaWriter = JavaWriter.inPackage(className.packageName());

    ClassWriter classWriter = javaWriter.addClass(className.simpleName());
    classWriter.addModifiers(PUBLIC, FINAL);
    writeNoInstanceConstructor(classWriter);
    writeRegistryList(classWriter, extensions);

    return javaWriter;
  }

  private void writeRegistryList(ClassWriter classWriter, Set<String> extensions) {
    List<String> extensionList = Ordering.natural().immutableSortedCopy(extensions);

    // Create a snippet which represents the comma-separated list of class name references.
    Snippet extensionClassList =
        Snippet.format(Joiner.on(", ").join(Collections.nCopies(extensions.size(), "%s.class")),
            Lists.transform(extensionList, new Function<String, TypeName>() {
              @Override public TypeName apply(String s) {
                return ClassName.bestGuessFromString(s);
              }
            }));

    TypeName collectionsType = TypeNames.forClass(Collections.class);
    TypeName arraysType = TypeNames.forClass(Arrays.class);
    Snippet immutableExtensionClassList =
        Snippet.format("%s.unmodifiableList(%s.asList(%s))", collectionsType, arraysType,
            extensionClassList);

    TypeName wildcardList = ParameterizedTypeName.create(List.class, WildcardName.create());
    FieldWriter extensionWriter = classWriter.addField(wildcardList, "EXTENSIONS");
    extensionWriter.addModifiers(PUBLIC, STATIC, FINAL);
    extensionWriter.setInitializer(immutableExtensionClassList);
  }
}
