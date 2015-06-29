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

import com.squareup.protoparser.ExtendElement;
import com.squareup.protoparser.OptionElement;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ServiceElement;
import com.squareup.protoparser.TypeElement;
import java.util.ArrayList;
import java.util.List;

public final class WireProtoFile {
  private final ProtoFile protoFile;
  private final List<WireType> types = new ArrayList<WireType>();
  private final List<WireService> services = new ArrayList<WireService>();
  private final List<WireExtend> wireExtends = new ArrayList<WireExtend>();
  private final List<WireOption> options = new ArrayList<WireOption>();

  public WireProtoFile(ProtoFile protoFile) {
    this.protoFile = protoFile;

    String protoPackage = protoFile.packageName();

    for (TypeElement type : protoFile.typeElements()) {
      ProtoTypeName protoTypeName = ProtoTypeName.get(protoPackage, type.name());
      types.add(WireType.get(protoTypeName, type));
    }

    for (ServiceElement service : protoFile.services()) {
      ProtoTypeName protoTypeName = ProtoTypeName.get(protoPackage, service.name());
      services.add(new WireService(protoTypeName, service));
    }

    for (ExtendElement extend : protoFile.extendDeclarations()) {
      wireExtends.add(new WireExtend(extend));
    }

    for (OptionElement option : protoFile.options()) {
      options.add(new WireOption(option));
    }
  }

  public String packageName() {
    return protoFile.packageName();
  }

  public List<WireType> types() {
    return types;
  }

  public List<WireService> services() {
    return services;
  }

  public List<WireExtend> wireExtends() {
    return wireExtends;
  }

  public List<WireOption> options() {
    return options;
  }

  public void register(Linker linker) {
    linker = linker.withProtoPackage(packageName());
    for (WireType type : types) {
      type.register(linker);
    }
  }

  public void link(Linker linker) {
    linker = linker.withProtoPackage(packageName());
    for (WireType type : types) {
      type.link(linker);
    }
    for (WireService service : services) {
      service.link(linker);
    }
    for (WireExtend extend : wireExtends) {
      extend.link(linker);
    }
  }
}
