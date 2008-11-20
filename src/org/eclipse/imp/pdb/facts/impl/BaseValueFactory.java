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

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IDouble;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IObject;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public abstract class BaseValueFactory implements IValueFactory {
    public IInteger integer(int i) {
        return new IntegerValue(i);
    }
    
    public IDouble dubble(double d) {
        return new DoubleValue(d);
    }
    
    public IString string(String s) {
        return new StringValue(s);
    }
    
    public ISourceLocation sourceLocation(String path, ISourceRange range) {
        return new SourceLocationValue(path, range);
    }
    
    public ISourceRange sourceRange(int startOffset, int length, int startLine, int endLine, int startCol, int endCol) {
        return new SourceRangeValue(startOffset, length, startLine, endLine, startCol, endCol);
    }
    
    public <T> IObject<T> object(T o) {
		Type type = TypeFactory.getInstance().objectType(o.getClass());
		return new ObjectValue<T>(type, o);
	}
    
    public IBool False() {
    	return new BoolValue(false);
    }
    
    public IBool True() {
    	return new BoolValue(true);
    }
}
