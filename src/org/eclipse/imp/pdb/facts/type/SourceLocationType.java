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

import java.net.URL;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;


/*package*/ final class SourceLocationType  extends Type {
    private final static SourceLocationType sInstance= new SourceLocationType();

    /*package*/ static SourceLocationType getInstance() {
        return sInstance;
    }

    private SourceLocationType() { }

    @Override
    public boolean isSourceLocationType() {
    	return true;
    }
    
    /**
     * Should never need to be called; there should be only one instance of IntegerType
     */
    @Override
    public boolean equals(Object obj) {
        return (obj instanceof SourceLocationType);
    }

    @Override
    public int hashCode() {
        return 61547;
    }

    @Override
    public String toString() {
        return "sourceLocation";
    }
    
    @Override
    public <T> T accept(ITypeVisitor<T> visitor) {
    	return visitor.visitSourceLocation(this);
    }
    
    @Override 
    public IValue make(IValueFactory f, URL url, int startOffset, int length,
    		int startLine, int endLine, int startCol, int endCol) {
    	return f.sourceLocation(url, startOffset, length, startLine, endLine, startCol, endCol);
    }
    
}
