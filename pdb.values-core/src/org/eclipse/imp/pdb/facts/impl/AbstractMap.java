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
package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.func.MapFunctions;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

public abstract class AbstractMap extends AbstractValue implements IMap {

    public AbstractMap() {
    	super();
    }

    protected static TypeFactory getTypeFactory() {
        return TypeFactory.getInstance();
    }

    protected static Type inferMapType(final Type candidateMapType, final java.util.Map<IValue, IValue> content) {
    	return inferMapType(candidateMapType, content.isEmpty());
    }
    
    protected static Type inferMapType(final Type candidateMapType, final boolean isEmpty) {
		if (!candidateMapType.isMap())
			throw new IllegalArgumentException("Type must be a map type: "
					+ candidateMapType);
    	
    	final Type inferredCollectionType;
                       
        // is collection empty?
        if (isEmpty) {
			inferredCollectionType = getTypeFactory().mapType(
					getTypeFactory().voidType(), candidateMapType.getKeyLabel(),
					getTypeFactory().voidType(), candidateMapType.getValueLabel());

        } else {
        	inferredCollectionType = candidateMapType;
        }

        return inferredCollectionType;
    }

    protected abstract IValueFactory getValueFactory();

    @Override
    public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
        return v.visitMap(this);
    }

	@Override
	public IMap put(IValue key, IValue value) {
		return MapFunctions.put(getValueFactory(), this, key, value);
	}

	@Override
	public boolean containsKey(IValue key) {
		return MapFunctions.containsKey(getValueFactory(), this, key);
	}

	@Override
	public boolean containsValue(IValue value) {
		return MapFunctions.containsValue(getValueFactory(), this, value);
	}

	@Override
	public Type getKeyType() {
		return getType().getKeyType();
	}

	@Override
	public Type getValueType() {
		return getType().getValueType();
	}

	@Override
	public IMap join(IMap that) {
		return MapFunctions.join(getValueFactory(), this, that);
	}

	@Override
	public IMap remove(IMap that) {
		return MapFunctions.remove(getValueFactory(), this, that);	
	}

	@Override
	public IMap compose(IMap that) {
		return MapFunctions.compose(getValueFactory(), this, that);
	}

	@Override
	public IMap common(IMap that) {
		return MapFunctions.common(getValueFactory(), this, that);
	}

	@Override
	public boolean isSubMap(IMap that) {
		return MapFunctions.isSubMap(getValueFactory(), this, that);
	}

}
