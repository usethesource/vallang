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

import java.net.URL;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IDouble;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValueFactory;

public abstract class BaseValueFactory implements IValueFactory {
    public IInteger integer(int i) {
        return new IntegerValue(i);
    }
    
    public IDouble dubble(double d) {
        return new DoubleValue(d);
    }
    
    public IString string(String s) {
    	if (s == null) {
    		throw new NullPointerException();
    	}
        return new StringValue(s);
    }
    
    public ISourceLocation sourceLocation(URL path, int startOffset, int length, int startLine, int endLine, int startCol, int endCol) {
    	if (path == null) {
    		throw new NullPointerException();
    	}
    	if (startOffset < 0 || length < 0 || startLine < 0 || endLine < startLine || endCol < 0 || (startLine == endLine && endCol < startCol)) {
    		throw new IllegalArgumentException();
    	}
        return new SourceLocationValue(path, startOffset, length, startLine, endLine, startCol, endCol);
    }
    
    public IBool bool(boolean value) {
    	return new BoolValue(value);
    }
}
