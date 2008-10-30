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

public class ValueType extends Type {
    private static final ValueType sInstance= new ValueType();

    public static ValueType getInstance() {
        return sInstance;
    }

    private ValueType() { }

    @Override
    public boolean isValueType() {
    	return true;
    }
    
    @Override
    public String getTypeDescriptor() {
        return toString();
    }

    @Override
    public boolean isSubtypeOf(Type other) {
        return other == this;
    }

    @Override
    public Type lub(Type other) {
        return this;
    }

    @Override
    public String toString() {
        return "value";
    }
    
    /**
     * Should never be called, ValueType is a singleton 
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof ValueType);
    }
    
    @Override
    public int hashCode() {
    	return 2141;
    }
}
