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

import java.util.HashSet;
import java.util.Iterator;
import java.util.function.Supplier;

import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWriter;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

/*package*/ class SetWriter implements ISetWriter {
    protected final HashSet<IValue> setContent;
    protected Type eltType;
    protected Set constructedSet;

    /*package*/ SetWriter() {
        super();
        this.eltType = TypeFactory.getInstance().voidType();
        setContent = new HashSet<>();
    }

    @Override
    public Iterator<IValue> iterator() {
        return setContent.iterator();
    }
    
    private void put(IValue elem) {
        updateType(elem);
        setContent.add(elem);
    }

    private void updateType(IValue elem) {
        eltType = eltType.lub(elem.getType());
    }

    @Override
    public void insertTuple(IValue... fields) {
        insert(ValueFactory.getInstance().tuple(fields));
    }
    
    @Override
	public void insert(IValue... elems) throws FactTypeUseException {
        checkMutation();

        for (IValue elem : elems) {
            put(elem);
        }
    }

    @Override
	public void insertAll(Iterable<? extends IValue> collection) throws FactTypeUseException {
        checkMutation();

        for (IValue v : collection) {
            put(v);
        }
    }

    @Override
	public ISet done() {
        if (constructedSet == null) {
            constructedSet = new Set(eltType, setContent);
        }

        return constructedSet;
    }

    private void checkMutation() {
        if (constructedSet != null)
            throw new UnsupportedOperationException("Mutation of a finalized set is not supported.");
    }
    
    @Override
    public Supplier<IWriter<ISet>> supplier() {
        return () -> ValueFactory.getInstance().setWriter();
    }
}
