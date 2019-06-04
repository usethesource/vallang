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
 *******************************************************************************/
package io.usethesource.vallang.impl;

import java.util.Random;

import io.usethesource.vallang.IList;
import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.visitors.IValueVisitor;

public abstract class AbstractList extends AbstractValue implements IList {

    public AbstractList() {
        super();
    }

    protected static TypeFactory getTypeFactory() {
        return TypeFactory.getInstance();
    }

    protected static Type inferListOrRelType(final Type elementType, final Iterable<IValue> content) {
    	return inferListOrRelType(elementType, content.iterator().hasNext() == false);
    }
    
    /*
     * TODO: get rid of code duplication (@see AbstractSet.inferSetOrRelType)
     */
    protected static Type inferListOrRelType(final Type elementType, final boolean isEmpty) {
        final Type inferredElementType;
        final Type inferredCollectionType;

        // is collection empty?
        if (isEmpty) {
            inferredElementType = getTypeFactory().voidType();
        } else {
            inferredElementType = elementType;
        }

        // consists collection out of tuples?
        if (inferredElementType.isFixedWidth()) {
            inferredCollectionType = getTypeFactory().lrelTypeFromTuple(inferredElementType);
        } else {
            inferredCollectionType = getTypeFactory().listType(inferredElementType);
        }

        return inferredCollectionType;
    }

    protected abstract IValueFactory getValueFactory();

    @Override
    public Type getElementType() {
        return getType().getElementType();
    }

    

	@Override
	public boolean isRelation() {
		return getType().isListRelation();
	}

	@Override
	public IRelation<IList> asRelation() {
		if (!isRelation()) {
			throw new IllegalOperationException("Cannot be viewed as a relation.", getType());
		}

		return new DefaultRelationViewOnList(this);
	}    
    
}
