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

    @Override
    public String toString() {
        return "\"" + fValue.replaceAll("\"", "\\\"") + "\"";
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
}
