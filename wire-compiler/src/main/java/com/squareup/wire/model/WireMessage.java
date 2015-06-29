/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.model;

import com.squareup.protoparser.ExtensionsElement;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.OneOfElement;
import com.squareup.protoparser.OptionElement;
import com.squareup.protoparser.TypeElement;
import java.util.ArrayList;
import java.util.List;

public final class WireMessage extends WireType {
  private final ProtoTypeName protoTypeName;
  private final MessageElement element;
  private final List<WireField> fields = new ArrayList<WireField>();
  private final List<WireOneOf> oneOfs = new ArrayList<WireOneOf>();
  private final List<WireType> nestedTypes = new ArrayList<WireType>();
  private final List<WireOption> options = new ArrayList<WireOption>();

  public WireMessage(ProtoTypeName protoTypeName, MessageElement element) {
    this.protoTypeName = protoTypeName;
    this.element = element;

    for (FieldElement field : element.fields()) {
      fields.add(new WireField(field));
    }

    for (OneOfElement oneOf : element.oneOfs()) {
      oneOfs.add(new WireOneOf(oneOf));
    }

    for (TypeElement type : element.nestedElements()) {
      nestedTypes.add(WireType.get(protoTypeName.nestedType(type.name()), type));
    }

    for (OptionElement option : element.options()) {
      options.add(new WireOption(option));
    }
  }

  @Override ProtoTypeName protoTypeName() {
    return protoTypeName;
  }

  @Override public String documentation() {
    return element.documentation();
  }

  public List<WireField> fields() {
    return fields;
  }

  public List<WireOneOf> oneOfs() {
    return oneOfs;
  }

  @Override public List<WireType> nestedTypes() {
    return nestedTypes;
  }

  public List<ExtensionsElement> extensions() {
    return element.extensions();
  }

  @Override public List<WireOption> options() {
    return options;
  }

  @Override void register(Linker linker) {
    linker.register(this);
    for (WireType type : nestedTypes) {
      type.register(linker);
    }
  }

  @Override public void link(Linker linker) {
    linker = linker.withMessage(this);
    for (WireField field : fields) {
      field.link(linker);
    }
    for (WireOneOf oneOf : oneOfs) {
      oneOf.link(linker);
    }
    for (WireType type : nestedTypes) {
      type.link(linker);
    }
  }
}
