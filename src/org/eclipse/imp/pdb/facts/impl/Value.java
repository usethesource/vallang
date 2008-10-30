/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public abstract class Value implements IValue {
    protected Map<String, IValue> fAnnotations = new HashMap<String, IValue>();
    
	/**
     * The type of this value
     */
    protected final Type fType;

    protected Value(Type type) {
    	fType= type;
    }

	/**
     * @return the type of this value
     */
    public Type getType() {
    	return fType;
    }
    
    /**
	 * @return the smallest super type of this type that is not a NamedType.
	 */
    public Type getBaseType() {
    	return fType.getBaseType();
    }
    
    public boolean hasAnnotation(String label) {
    	return fAnnotations.containsKey(label);
    }
    
    public IValue setAnnotation(String label, IValue value) {
    	Value clone;
    	
    	Type expected = TypeFactory.getInstance().getAnnotationType(getType(), label);
    	
    	if (expected == null) {
    		throw new FactTypeError("This annotation was not declared for this type: " + label + " for " + getType());
    	}
    		
        if (!value.getType().isSubtypeOf(expected)) {
    		throw new FactTypeError("The type of this annotation should be a subtype of " + expected + " and not " + value.getType());
    	}
        
		try {
			clone = (Value) clone();
			clone.fAnnotations.put(label, value);
	    	return clone;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Internal error: all IValues should implement clone method", e);
		}
    }
    
    public IValue getAnnotation(String label) {
    	return fAnnotations.get(label);
    }
    
    /**
     * clone must be implemented in order to make setAnnotation implementable
     * without mutating the original object.
     */
    @Override
    protected abstract Object clone() throws CloneNotSupportedException;
}
