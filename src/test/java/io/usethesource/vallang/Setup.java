/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.usethesource.vallang.impl.persistent.ValueFactory;
import io.usethesource.vallang.io.binary.message.IValueReader;
import io.usethesource.vallang.type.TypeStore;

public class Setup {
  
  public static Iterable<? extends Object> valueFactories() {
    final String propertyName = String.format("%s.%s", Setup.class.getName(), "valueFactory");
    final String propertyValue = System.getProperty(propertyName, "REFERENCE,PERSISTENT");

    final IValueFactory[] valueFactories =
        Stream.of(propertyValue.split(",")).map(String::trim).map(ValueFactoryEnum::valueOf)
            .map(ValueFactoryEnum::getInstance).toArray(IValueFactory[]::new);

    return Arrays.asList(valueFactories);
  }

  /**
   * Allocates new {@link TypeStore} environments that are used within {@link IValueReader}.
   *
   * Type stores are used as encapsulated namespaces for types. The supplier creates a fresh type
   * store environment, to avoid name clashes when nesting types / values.
   */
  public static final Supplier<TypeStore> TYPE_STORE_SUPPLIER = () -> {
    TypeStore typeStore = new TypeStore();
    // typeStore.declareAbstractDataType(RascalValueFactory.Type);
    // typeStore.declareConstructor(RascalValueFactory.Type_Reified);
    // typeStore.declareAbstractDataType(RascalValueFactory.ADTforType);
    return typeStore;
  };

  private enum ValueFactoryEnum {
    REFERENCE {
      @Override
      public IValueFactory getInstance() {
        return io.usethesource.vallang.impl.reference.ValueFactory.getInstance();
      }
    },
    PERSISTENT {
      @Override
      public IValueFactory getInstance() {
        return ValueFactory.getInstance();
      }
    };

    public abstract IValueFactory getInstance();
  }

}
