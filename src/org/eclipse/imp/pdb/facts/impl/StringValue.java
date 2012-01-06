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

import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class StringValue extends Value implements IString {
    private final String fValue;

    /*package*/ StringValue(String value) {
        super(TypeFactory.getInstance().stringType());
        if (value == null) {
            throw new IllegalArgumentException("Null string value");
        }
        fValue= value;
    }

	public String getValue() {
        return fValue;
    }

    public IString concat(IString other) {
    	return new StringValue(fValue.concat(other.getValue()));
    }
    
    public int compare(IString other) {
    	int result = fValue.compareTo(other.getValue());
    	return result < 0 ? -1 : (result > 0) ? 1 : 0;
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o == null) {
    		return false;
    	}
    	if (this == o) {
    		return true;
    	}
    	if (getClass() == o.getClass()) {
    		return ((StringValue) o).fValue.equals(fValue);
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return fValue.hashCode();
    }
    
    public <T> T accept(IValueVisitor<T> v) throws VisitorException {
    	return v.visitString(this);
    }

	public IString reverse() {
		StringBuilder b = new StringBuilder(fValue);
		return new StringValue(b.reverse().toString());
	}

	public int length() {
		return fValue.codePointCount(0, fValue.length());
	}

	private int codePointAt(java.lang.String str, int i) {
		return str.codePointAt(str.offsetByCodePoints(0,i));
	}
	
	public IString substring(int start, int end) {
		 return new StringValue(fValue.substring(fValue.offsetByCodePoints(0, start),fValue.offsetByCodePoints(0, end)));
	}
	
	public IString substring(int start) {
		 return new StringValue(fValue.substring(fValue.offsetByCodePoints(0, start)));
	}

	public int charAt(int index) {
		return codePointAt(fValue, index);
	}
}
