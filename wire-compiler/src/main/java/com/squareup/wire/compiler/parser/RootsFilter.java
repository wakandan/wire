package com.squareup.wire.compiler.parser;

import com.squareup.protoparser.EnumElement;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.OneOfElement;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.RpcElement;
import com.squareup.protoparser.ServiceElement;
import com.squareup.protoparser.TypeElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.squareup.protoparser.Scalars.isScalarType;

/**
 * Filter a set of proto file objects to only include the specified types and their transitive
 * dependencies.
 * <p>
 * This is done in three steps:
 * <ol>
 * <li>Decompose the proto file object hierarchy into a tree of nodes that represents each type and
 * service.</li>
 * <li>For each root type, find its corresponding node and mark it to be kept. Doing so will also
 * mark all of its parent types (if any) and its dependencies (if any) to be kept as well.</li>
 * <li>Re-assemble only the kept nodes back into proto file objects.</li>
 * </ol>
 */
final class RootsFilter {
  /** Filter the protos to only include the specified types and their transitive dependencies. */
  static Set<ProtoFile> filter(Set<ProtoFile> protoFiles, Set<String> roots) {
    // Transform the set of proto files into a tree of nodes.
    RootNode rootNode = new RootNode(protoFiles);
    Map<String, Node<?>> nodeMap = rootNode.asNodeMap();

    // Collect nodes to keep by starting at the supplied roots and transitively iterating out.
    Set<Node<?>> nodesToKeep = new LinkedHashSet<>();
    for (String root : roots) {
      if (!nodeMap.containsKey(root)) {
        throw new IllegalStateException("Unknown type " + root);
      }
      nodeMap.get(root).keepNodes(nodesToKeep, nodeMap);
    }

    // Re-assemble all of the marked nodes back into a set of proto files.
    return rootNode.collectKeptNodes(nodesToKeep);
  }

  private static Node<?> nodeForType(Node<?> parent, TypeElement type) {
    if (type instanceof MessageElement) {
      return new MessageTypeNode(parent, (MessageElement) type);
    }
    if (type instanceof EnumElement) {
      return new EnumTypeNode(parent, (EnumElement) type);
    }
    throw new IllegalArgumentException("Unknown type " + type.getClass().getCanonicalName());
  }

  private abstract static class Node<T> {
    final Node<?> parent;
    final String type;
    final T obj;
    final List<Node<?>> children;

    Node(Node<?> parent, String type, T obj) {
      this.parent = parent;
      this.type = type;
      this.obj = obj;

      children = new ArrayList<>();
    }

    /** Flatten this type and the types of any children into a map to their corresponding nodes. */
    final Map<String, Node<?>> asNodeMap() {
      Map<String, Node<?>> typeMap = new LinkedHashMap<>();
      if (type != null) {
        typeMap.put(type, this);
      }
      for (Node<?> child : children) {
        typeMap.putAll(child.asNodeMap());
      }
      return typeMap;
    }

    /** Create a real proto object of this type and any children present in the supplied set. */
    abstract T collectKeptNodes(Set<Node<?>> typesToKeep);

    /** Mark this node to be kept. This method should be overriden to keep any dependencies. */
    void keepNodes(Set<Node<?>> typesToKeep, Map<String, Node<?>> nodeMap) {
      if (typesToKeep.contains(this)) {
        return;
      }
      typesToKeep.add(this);
      if (parent != null) {
        parent.keepNodes(typesToKeep, nodeMap);
      }
    }
  }

  /** The root node which represents set of {@link ProtoFile} objects. */
  private static class RootNode extends Node<Set<ProtoFile>> {
    RootNode(Set<ProtoFile> protoFiles) {
      super(null, null, protoFiles);

      for (ProtoFile protoFile : protoFiles) {
        children.add(new ProtoFileNode(this, protoFile));
      }
    }

    @Override public Set<ProtoFile> collectKeptNodes(Set<Node<?>> typesToKeep) {
      Set<ProtoFile> protoFiles = new LinkedHashSet<>();
      for (Node<?> child : children) {
        if (typesToKeep.contains(child)) {
          protoFiles.add((ProtoFile) child.collectKeptNodes(typesToKeep));
        }
      }
      return protoFiles;
    }
  }

  private static class ProtoFileNode extends Node<ProtoFile> {
    ProtoFileNode(RootNode parent, ProtoFile protoFile) {
      super(parent, null, protoFile);

      for (TypeElement type : protoFile.typeElements()) {
        children.add(nodeForType(this, type));
      }
      for (ServiceElement service : protoFile.services()) {
        children.add(new ServiceNode(this, service));
      }
    }

    @Override ProtoFile collectKeptNodes(Set<Node<?>> typesToKeep) {
      List<TypeElement> markedTypes = new ArrayList<>();
      List<ServiceElement> markedServices = new ArrayList<>();
      for (Node<?> child : children) {
        if (typesToKeep.contains(child)) {
          if (child instanceof ServiceNode) {
            markedServices.add((ServiceElement) child.collectKeptNodes(typesToKeep));
          } else {
            markedTypes.add((TypeElement) child.collectKeptNodes(typesToKeep));
          }
        }
      }
      return ProtoFile.create(obj.filePath(), obj.packageName(), obj.dependencies(),
          obj.publicDependencies(), markedTypes, markedServices, obj.extendDeclarations(),
          obj.options());
    }
  }

  private static class ServiceNode extends Node<ServiceElement> {
    ServiceNode(Node<?> parent, ServiceElement type) {
      super(parent, type.qualifiedName(), type);
    }

    @Override void keepNodes(Set<Node<?>> typesToKeep, Map<String, Node<?>> nodeMap) {
      super.keepNodes(typesToKeep, nodeMap);

      for (RpcElement method : obj.rpcs()) {
        String requestType = method.requestType();
        if (!isScalarType(requestType)) {
          nodeMap.get(requestType).keepNodes(typesToKeep, nodeMap);
        }
        String responseType = method.responseType();
        if (!isScalarType(responseType)) {
          nodeMap.get(responseType).keepNodes(typesToKeep, nodeMap);
        }
      }
    }

    @Override ServiceElement collectKeptNodes(Set<Node<?>> typesToKeep) {
      return obj; // No child types that could possibly be filtered. Return the original.
    }
  }

  private static class MessageTypeNode extends Node<MessageElement> {
    MessageTypeNode(Node<?> parent, MessageElement type) {
      super(parent, type.qualifiedName(), type);

      for (TypeElement nestedType : type.nestedElements()) {
        children.add(nodeForType(this, nestedType));
      }
    }

    @Override void keepNodes(Set<Node<?>> typesToKeep, Map<String, Node<?>> nodeMap) {
      super.keepNodes(typesToKeep, nodeMap);

      for (FieldElement field : obj.fields()) {
        String fieldType = field.type();
        if (!isScalarType(fieldType)) {
          nodeMap.get(fieldType).keepNodes(typesToKeep, nodeMap);
        }
      }
    }

    @Override MessageElement collectKeptNodes(Set<Node<?>> typesToKeep) {
      List<TypeElement> markedNestedTypes = new ArrayList<>();
      for (Node<?> child : children) {
        if (typesToKeep.contains(child)) {
          markedNestedTypes.add((TypeElement) child.collectKeptNodes(typesToKeep));
        }
      }
      return MessageElement.create(obj.name(), obj.qualifiedName(), obj.documentation(),
          obj.fields(), Collections.<OneOfElement>emptyList(), markedNestedTypes, obj.extensions(),
          obj.options());
    }
  }

  private static class EnumTypeNode extends Node<EnumElement> {
    EnumTypeNode(Node<?> parent, EnumElement type) {
      super(parent, type.qualifiedName(), type);
    }

    @Override EnumElement collectKeptNodes(Set<Node<?>> typesToKeep) {
      return obj; // No child types that could possibly be filtered. Return the original.
    }
  }

  private RootsFilter() {
    throw new AssertionError("No instances.");
  }
}
