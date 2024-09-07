/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *
 * Based on code by:
 *
 *   * Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/
package io.usethesource.vallang.impl.reference;

import java.util.Iterator;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.type.Type;

/*package*/ class Set implements ISet {
    final Type type;
    final java.util.Set<IValue> content;

    /*package*/ Set(Type elementType, java.util.Set<IValue> content) {
        super();
        this.type = TF.setType(elementType);
        this.content = content;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean contains(IValue e) {
        return content.contains(e);
    }

    @Override
    public ISetWriter writer() {
        return ValueFactory.getInstance().setWriter();
    }

    @Override
    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public int size() {
        return content.size();
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == null) {
            return false;
        }
        return defaultEquals(other);
    }

    @Override
    public String toString() {
        return defaultToString();
    }

    @Override
    public Iterator<IValue> iterator() {
        return content.iterator();
    }
}
