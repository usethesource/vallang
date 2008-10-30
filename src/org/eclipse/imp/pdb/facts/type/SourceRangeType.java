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

public class SourceRangeType extends Type {
    private static final SourceRangeType sInstance= new SourceRangeType();

    public static SourceRangeType getInstance() {
        return sInstance;
    }

    private SourceRangeType() { }

    @Override
    public boolean isSourceRangeType() {
    	return true;
    }
    
    @Override
    public String getTypeDescriptor() {
        return toString();
    }

    @Override
    public boolean isSubtypeOf(Type other) {
        return other == this || other.isValueType();
    }

    @Override
    public Type lub(Type other) {
        if (other.isSubtypeOf(this)) {
            return this;
        }
        return TypeFactory.getInstance().valueType();
    }

    @Override
    public String toString() {
        return "sourceRange";
    }
}
