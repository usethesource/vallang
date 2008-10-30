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
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class StringValue extends Value implements IString {
    private final String fValue;

    /*package*/ StringValue(String value) {
        super(TypeFactory.getInstance().stringType());
        fValue= value;
    }

    /*package*/ StringValue(NamedType type, String s) {
		super(type);
		fValue = s;
	}

	public String getValue() {
        return fValue;
    }

    @Override
    public String toString() {
        return "\"" + fValue.replaceAll("\"", "\\\"") + "\"";
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof StringValue) {
    		return ((StringValue) o).fValue.equals(fValue);
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return fValue.hashCode();
    }
    
    public IValue accept(IValueVisitor v) throws VisitorException {
    	return v.visitString(this);
    }
    
    @Override
    protected Object clone() throws CloneNotSupportedException {
    	return new StringValue(fValue);
    }
}
