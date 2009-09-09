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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactParseError;

public abstract class BaseValueFactory implements IValueFactory {
    public IInteger integer(int i) {
        return new IntegerValue(i);
    }
    
    public IInteger integer(long l) {
    	return new IntegerValue(l);
    }
    
    public IInteger integer(byte[] a) {
    	return new IntegerValue(a);
    }
    
    public IInteger integer(String s) {
    	return new IntegerValue(new BigInteger(s));
    }
    
    public IReal real(double d) {
        return new RealValue(d);
    }
    
    public IReal real(float f) {
    	return new RealValue(f);
    }
    
    public IReal real(String s) {
    	return new RealValue(new BigDecimal(s));
    }
    
    public IString string(String s) {
    	if (s == null) {
    		throw new NullPointerException();
    	}
        return new StringValue(s);
    }
    
    public ISourceLocation sourceLocation(URI path, int startOffset, int length, int startLine, int endLine, int startCol, int endCol) {
    	if (path == null) {
    		throw new NullPointerException();
    	}
    	if (startOffset < 0 || length < 0 || startLine < 0 || endLine < startLine || endCol < 0 || (startLine == endLine && endCol < startCol)) {
    		throw new IllegalArgumentException();
    	}
        return new SourceLocationValue(path, startOffset, length, startLine, endLine, startCol, endCol);
    }
    
    public ISourceLocation sourceLocation(String path, int startOffset, int length, int startLine, int endLine, int startCol, int endCol) {
    	try {
    		if (!path.startsWith("/")) {
    			path = "/" + path;
    		}
			return sourceLocation(new URI("file://" + path), startOffset, length, startLine, endLine, startCol, endCol);
		} catch (URISyntaxException e) {
			throw new FactParseError("illegal path syntax", e);
		}
    }
    
    public ISourceLocation sourceLocation(URI url) {
		return sourceLocation(url, -1, -1, -1, -1, -1, -1);
	}
	
	public ISourceLocation sourceLocation(String path) {
		return sourceLocation(path, -1, -1, -1, -1, -1, -1);
	}
    
    public IBool bool(boolean value) {
    	return new BoolValue(value);
    }
}
