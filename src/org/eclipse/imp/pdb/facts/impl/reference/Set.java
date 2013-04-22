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
package org.eclipse.imp.pdb.facts.impl.reference;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.AbstractSet;
import org.eclipse.imp.pdb.facts.type.Type;

import java.util.Iterator;

/*package*/ class Set extends AbstractSet {

    final java.util.Set<IValue> content;

    /*package*/ Set(Type elementType, java.util.Set<IValue> content) {
        super(inferSetOrRelType(elementType, content));

        this.content = content;
    }

    @Override
    protected IValueFactory getValueFactory() {
        return ValueFactory.getInstance();
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
    public boolean contains(IValue e) {
        return content.contains(e);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public Iterator<IValue> iterator() {
        return content.iterator();
    }

}
