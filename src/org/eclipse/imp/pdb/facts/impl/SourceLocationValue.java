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

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class SourceLocationValue extends Value implements ISourceLocation {
    private final String fPath;
    private final ISourceRange fRange;

    /*package*/ SourceLocationValue(String path, ISourceRange range) {
        super(TypeFactory.getInstance().sourceLocationType());
        fPath= path;
        fRange= range;
    }

    /*package*/ SourceLocationValue(NamedType type, String path, ISourceRange range) {
		super(type);
		fPath = path;
		fRange = range;
	}

	public String getPath() {
        return fPath;
    }

    public ISourceRange getRange() {
        return fRange;
    }

    @Override
    public String toString() {
        return "<srcLoc: " + fPath + ":" + fRange + ">";
    }
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof SourceLocationValue) {
    		SourceLocationValue other = (SourceLocationValue) o;
    		return other.fPath.equals(fPath) && other.fRange.equals(fRange);
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	return 10987 + 11923 * fPath.hashCode() + 9619 * fRange.hashCode();
    }
    
    public IValue accept(IValueVisitor v) throws VisitorException {
    	return v.visitSourceLocation(this);
    }
    
    @Override
    protected Object clone() throws CloneNotSupportedException {
    	if (getType() instanceof NamedType) {
    	  return new SourceLocationValue((NamedType) getType(), fPath, fRange);
    	}
    	else {
    		return new SourceLocationValue(fPath, fRange);
    	}
    }
}
