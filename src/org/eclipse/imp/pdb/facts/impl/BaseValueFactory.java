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

import org.eclipse.imp.pdb.facts.IDouble;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IObject;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.ObjectType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public abstract class BaseValueFactory implements IValueFactory {
    public IInteger integer(int i) {
        return new IntegerValue(i);
    }
    
    public IInteger integer(NamedType type, int i) throws FactTypeError {
    	if (!type.getBaseType().isIntegerType()) {
    		throw new FactTypeError(type + " is not an integer type");
    	}
    	return new IntegerValue(type, i);
    }

    public IDouble dubble(double d) {
        return new DoubleValue(d);
    }
    
    public IDouble dubble(NamedType type, double d) throws FactTypeError {
    	if (!type.getBaseType().isDoubleType()) {
    		throw new FactTypeError(type + " is not a double type");
    	}
    	return new DoubleValue(type, d);
    }

    public IString string(String s) {
        return new StringValue(s);
    }
    
    public IString string(NamedType type, String s) throws FactTypeError {
    	if (!type.getBaseType().isStringType()) {
    		throw new FactTypeError(type + " is not a string type");
    	}
    	return new StringValue(type, s);
    }

    public ISourceLocation sourceLocation(String path, ISourceRange range) {
        return new SourceLocationValue(path, range);
    }
    
    public ISourceLocation sourceLocation(NamedType type, String path, ISourceRange range) throws FactTypeError {
    	if (!type.getBaseType().isSourceLocationType()) {
    		throw new FactTypeError(type + " is not a source location type");
    	}
    	return new SourceLocationValue(type, path, range);
    }

    public ISourceRange sourceRange(int startOffset, int length, int startLine, int endLine, int startCol, int endCol) {
        return new SourceRangeValue(startOffset, length, startLine, endLine, startCol, endCol);
    }
    
    public ISourceRange sourceRange(NamedType type, int startOffset, int length, int startLine, int endLine, int startCol, int endCol) throws FactTypeError {
    	if (!type.getBaseType().isSourceRangeType()) {
    		throw new FactTypeError(type + " is not a source range type");
    	}
    	return new SourceRangeValue(type, startOffset, length, startLine, endLine, startCol, endCol);
    }
    
    public <T> IObject<T> object(T o) {
		Type type = TypeFactory.getInstance().objectType(o.getClass());
		return new ObjectValue<T>(type, o);
	}

	@SuppressWarnings("unchecked")
	public <T> IObject<T> object(NamedType type, T o) throws FactTypeError {
		if (!type.getBaseType().isObjectType()) {
			throw new FactTypeError(type + " is not an object type");
		}
		ObjectType<T> baseType = (ObjectType<T>) type.getBaseType();
		if (!baseType.checkClass((Class<T>) o.getClass())) {
			throw new FactTypeError(type + " is not compatible with " + o.getClass().getCanonicalName());
		}
		return new ObjectValue<T>(type, o);
	}
}
