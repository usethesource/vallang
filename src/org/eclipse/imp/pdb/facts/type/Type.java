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

package org.eclipse.imp.pdb.facts.type;

public abstract class Type {
    /**
     * @return the least upper bound type of the receiver and the argument type
     */
    public abstract Type lub(Type other);
    
    public abstract boolean isSubtypeOf(Type other);

    /**
     * @return a type descriptor suitable for use in serialization, which can be
     * passed to <code>TypeFactory.getTypeByDescriptor()</code>
     */
    public abstract String getTypeDescriptor();
    
    public Type getBaseType() {
    	return this;
    }
    
    public boolean isRelationType() {
    	return false;
    }
    
    public boolean isSetType() {
    	return false;
    }
    
    public boolean isTupleType() {
    	return false;
    }
    
    public boolean isListType() {
    	return false;
    }
    
    public boolean isIntegerType() {
    	return false;
    }
    
    public boolean isNumberType() {
    	return false;
    }
    
    public boolean isDoubleType() {
    	return false;
    }
    
    public boolean isStringType() {
    	return false;
    }
    
    public boolean isSourceLocationType() {
    	return false;
    }
    
    public boolean isSourceRangeType() {
    	return false;
    }
    
    public boolean isNamedType() {
    	return false;
    }
    
    public boolean isValueType() {
    	return false;
    }
    
    public boolean isObjectType() {
    	return false;
    }
    
    public boolean isTreeNodeType() {
    	return false;
    }

	public boolean isTreeSortType() {
		return false;
	}

	public boolean isMapType() {
		return false;
	}
}
