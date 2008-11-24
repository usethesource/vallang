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

import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

public class ValueType extends Type {
	private static class InstanceHolder {
		public static final ValueType sInstance= new ValueType();
	}
	
    public static ValueType getInstance() {
        return InstanceHolder.sInstance;
    }

    private ValueType() { }

    @Override
    public boolean isValueType() {
    	return true;
    }
    
    @Override
    public boolean isSubtypeOf(Type other) {
        return other == this;
    }

    @Override
    public Type lub(Type other) {
        return this;
    }

    @Override
    public String toString() {
        return "value";
    }
    
    /**
     * Should never be called, ValueType is a singleton 
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof ValueType);
    }
    
    @Override
    public int hashCode() {
    	return 2141;
    }
    
    @Override
    public <T> T accept(ITypeVisitor<T> visitor) {
    	return visitor.visitValue(this);
    }
    
    @Override
    public IValue make(IValueFactory f) {
    	// if we don't care what kind of value to make, but it
    	// should be something that is empty, we make the empty
    	// tuple
    	return TypeFactory.getInstance().tupleEmpty().make(f);
    }
    
    @Override
    public IValue make(IValueFactory f, double arg) {
    	return TypeFactory.getInstance().doubleType().make(f, arg);
    }
    
    @Override
    public IValue make(IValueFactory f, int arg) {
    	return TypeFactory.getInstance().integerType().make(f, arg);
    }
    
    
    @Override
    public IValue make(IValueFactory f, int startOffset, int length,
    		int startLine, int endLine, int startCol, int endCol) {
    	return TypeFactory.getInstance().sourceRangeType().make(f, startOffset, length, startLine, endLine, startCol, endCol);
    }
    
    @Override
    public IValue make(IValueFactory f, IValue... args) {
    	// this could be anything that takes variable sized argument lists.
    	// for a default, lets construct a tuple:
    	return TypeFactory.getInstance().tupleType(args).make(f, args);
    }
    
    @Override
    public IValue make(IValueFactory f, String arg) {
    	return TypeFactory.getInstance().stringType().make(f, arg);
    }
    
    @Override
    public IValue make(IValueFactory f, String path, ISourceRange range) {
    	return TypeFactory.getInstance().sourceLocationType().make(f, path, range);
    }

    @Override
    public IValue make(IValueFactory f, String name, IValue... children) {
    	return f.tree(name, children);
    }
    
    @Override
    public IValue make(IValueFactory f, boolean arg) {
    	return TypeFactory.getInstance().boolType().make(f, arg);
    }
    
    @SuppressWarnings("unchecked")
	@Override
    public IListWriter writer(IValueFactory f) {
    	// if we don't care what kind of container to make
    	// we make a list of values
    	return (IListWriter) f.listWriter(TypeFactory.getInstance().valueType());
    }
}
