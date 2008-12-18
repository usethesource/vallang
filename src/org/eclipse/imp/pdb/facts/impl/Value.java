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

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public abstract class Value implements IValue {
	/**
	 * Keeps the annotations of this value 
	 */
    protected final HashMap<String, IValue> fAnnotations;
    
	/**
     * The type of this value
     */
    protected final Type fType;

    protected Value(Type type) {
    	fType= type;
    	fAnnotations = BaseValueFactory.EMPTY_ANNOTATIONS;
    }
    
	protected Value(Type type, HashMap<String, IValue> annotations) {
		fType= type;
    	fAnnotations = annotations.isEmpty() ? BaseValueFactory.EMPTY_ANNOTATIONS : annotations;
	}
    
    /**
     * Create new value with changed annotation. Used to implement clone() method.
     * @param other the other value
     * @param label the label of the annotation to set
     * @param anno  the new value of the annotation
     */
	protected Value(Value other, String label, IValue anno) {
    	this(other);
    	fAnnotations.put(label, anno);
    }
	
	public boolean isEqual(IValue other) {
		// TODO: reimplement this for all sub-types of Value
		return equals(other);
	}
	
   @SuppressWarnings("unchecked")
	public Value(Value other) {
		fType = other.fType;
		fAnnotations = (HashMap<String, IValue>) other.fAnnotations.clone();
	}

    protected boolean equalAnnotations(Value o) {
    	if (!fAnnotations.equals(o.fAnnotations)) {
    		return false;
    	}
    	return true;
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
    	Type expected = TypeFactory.getInstance().getAnnotationType(getType(), label);
    	
    	if (expected == null) {
    		throw new FactTypeError("This annotation was not declared for this type: " + label + " for " + getType());
    	}
    		
        if (!value.getType().isSubtypeOf(expected)) {
    		throw new FactTypeError("The type of this annotation should be a subtype of " + expected + " and not " + value.getType());
    	}
        
        return (Value) clone(label, value);
    }
    
    public IValue getAnnotation(String label) {
    	return fAnnotations.get(label);
    }
    
    /**
     * Should clone the value and set the annotation in the constructor.
     * @param label the label of the annotation to set
     * @param value the new value of the annotation
     * @return the exact same value, with the same type, but with a new value for the chosen annotation label
     */
    protected abstract IValue clone(String label, IValue value);

	
}
