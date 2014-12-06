package com.squareup.wire.compiler.parser;

import com.squareup.protoparser.EnumElement;
import com.squareup.protoparser.ExtendElement;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.OneOfElement;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.RpcElement;
import com.squareup.protoparser.ServiceElement;
import com.squareup.protoparser.TypeElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.squareup.protoparser.Scalars.isScalarType;
import static java.util.Collections.unmodifiableSet;

final class ProtoQualifier {
  /** Update a set of profo files to only refer to fully-qualified or primitive types. */
  static Set<ProtoFile> fullyQualifyProtos(Set<ProtoFile> protoFiles, Set<String> allTypes) {
    Set<ProtoFile> qualifiedProtoFiles = new LinkedHashSet<>(protoFiles.size());
    for (ProtoFile protoFile : protoFiles) {
      // Replace all types (including nested ones) with fully-qualified references.
      List<TypeElement> types = protoFile.typeElements();
      List<TypeElement> qualifiedTypes = new ArrayList<>(types.size());
      for (TypeElement type : types) {
        qualifiedTypes.add(fullyQualifyType(type, allTypes));
      }

      // Replace all services with fully-qualified references.
      List<ServiceElement> services = protoFile.services();
      List<ServiceElement> qualifiedServiceElements = new ArrayList<>(services.size());
      for (ServiceElement service : services) {
        qualifiedServiceElements.add(fullyQualifyServiceElement(service, allTypes));
      }

      // Replace all extend declarations with fully-qualified references.
      List<ExtendElement> extendDeclarations = protoFile.extendDeclarations();
      List<ExtendElement> qualifiedExtendElements = new ArrayList<>(extendDeclarations.size());
      for (ExtendElement extendDeclaration : extendDeclarations) {
        qualifiedExtendElements.add(
            fullyQualifyExtendElement(protoFile.packageName(), extendDeclaration, allTypes));
      }

      // Create a new proto file using our new types, services, and extends.
      qualifiedProtoFiles.add(ProtoFile.create(protoFile.filePath(), protoFile.packageName(),
          protoFile.dependencies(), protoFile.publicDependencies(), qualifiedTypes,
          qualifiedServiceElements, qualifiedExtendElements, protoFile.options()));
    }

    return unmodifiableSet(qualifiedProtoFiles);
  }

  /** Update a message or enum type to only refer to fully-qualified or primitive types. */
  static TypeElement fullyQualifyType(TypeElement type, Set<String> allTypes) {
    if (type instanceof MessageElement) {
      MessageElement messageType = (MessageElement) type;

      // Recurse to fully-qualify and nested types.
      List<TypeElement> nestedTypes = type.nestedElements();
      List<TypeElement> qualifiedNestedTypes = new ArrayList<>(nestedTypes.size());
      for (TypeElement nestedType : nestedTypes) {
        qualifiedNestedTypes.add(fullyQualifyType(nestedType, allTypes));
      }

      // Fully-qualify each field's type.
      String qualifiedName = messageType.qualifiedName();
      List<FieldElement> fields = messageType.fields();
      List<FieldElement> qualifiedFieldElements =
          fullyQualifyFieldElements(fields, qualifiedName, allTypes);

      // Create a new message using our new nested types and fields.
      return MessageElement.create(messageType.name(), messageType.qualifiedName(),
          messageType.documentation(), qualifiedFieldElements,
          Collections.<OneOfElement>emptyList(), qualifiedNestedTypes, messageType.extensions(),
          messageType.options());
    } else if (type instanceof EnumElement) {
      return type; // Enums don't have any type references that need qualified.
    } else {
      throw new IllegalArgumentException("Unknown type " + type.getClass().getCanonicalName());
    }
  }

  /** Update an service to only refer to fully-qualified or primitive types. */
  static ServiceElement fullyQualifyServiceElement(ServiceElement service, Set<String> allTypes) {
    String qualifiedName = service.qualifiedName();
    List<RpcElement> methods = service.rpcs();
    List<RpcElement> qualifiedRpcElements = new ArrayList<>(methods.size());
    for (RpcElement method : methods) {
      String newRequestType = resolveType(allTypes, qualifiedName, method.requestType());
      String newResponseType = resolveType(allTypes, qualifiedName, method.responseType());

      qualifiedRpcElements.add(
          RpcElement.create(method.name(), method.documentation(), newRequestType, newResponseType,
              method.options()));
    }

    return ServiceElement.create(service.name(), service.qualifiedName(), service.documentation(),
        service.options(), qualifiedRpcElements);
  }

  /** Update an extend declaration to only refer to fully-qualified or primitive types. */
  static ExtendElement fullyQualifyExtendElement(String scope, ExtendElement extendDeclaration,
      Set<String> allTypes) {
    List<FieldElement> fields = extendDeclaration.fields();
    List<FieldElement> qualifiedFieldElements = fullyQualifyFieldElements(fields, scope, allTypes);

    return ExtendElement.create(extendDeclaration.name(), extendDeclaration.qualifiedName(),
        extendDeclaration.documentation(), qualifiedFieldElements);
  }

  /** Update a list of fields to only refer to fully-qualified or primitive types. */
  private static List<FieldElement> fullyQualifyFieldElements(List<FieldElement> fields,
      String scope, Set<String> allTypes) {
    List<FieldElement> qualifiedFieldElements = new ArrayList<>(fields.size());
    for (FieldElement field : fields) {
      String newType = resolveType(allTypes, scope, field.type());

      qualifiedFieldElements.add(
          FieldElement.create(field.label(), newType, field.name(), field.tag(),
              field.documentation(), field.options()));
    }
    return qualifiedFieldElements;
  }

  /**
   * Given a set of all fully-qualified types, attempt to resolve the supplied type from the scope
   * (a package or fully-qualified message) into a fully-qualified type.
   * <p>
   * Type name resolution in the protocol buffer language works like C++: first the innermost scope
   * is searched, then the next-innermost, and so on, with each package considered to be "inner" to
   * its parent package. A leading '.' (for example, .foo.bar.Baz) means to start from the
   * outermost scope instead.
   */
  static String resolveType(Set<String> allTypes, String scope, String type) {
    if (isScalarType(type) || allTypes.contains(type)) {
      return type;
    }
    if (type.startsWith(".")) {
      type = type.substring(1);
      if (allTypes.contains(type)) {
        return type;
      }
    } else {
      String newScope = scope;
      while (newScope != null) {
        String newType = newScope + "." + type;
        if (allTypes.contains(newType)) {
          return newType;
        }
        int index = newScope.lastIndexOf('.');
        newScope = index == -1 ? null : newScope.substring(0, index);
      }
    }
    throw new IllegalArgumentException("Unknown type " + type + " in " + scope);
  }

  private ProtoQualifier() {
    throw new AssertionError("No instances.");
  }
}
