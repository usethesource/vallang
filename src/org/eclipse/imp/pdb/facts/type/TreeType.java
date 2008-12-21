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

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

/*package*/ final class TreeType extends Type {
	private static class InstanceHolder {
		public static final TreeType sInstance= new TreeType();
	}
	
    public static TreeType getInstance() {
        return InstanceHolder.sInstance;
    }

    private TreeType() { }

    @Override
    public boolean isTreeType() {
    	return true;
    }
    
    @Override
    public String toString() {
        return "tree";
    }
    
    /**
     * Should never be called, TreeType is a singleton 
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof TreeType);
    }
    
    @Override
    public int hashCode() {
    	return 20102;
    }
    
    @Override
    public <T> T accept(ITypeVisitor<T> visitor) {
    	return visitor.visitTree(this);
    }
    
    @Override
    public IValue make(IValueFactory f, String name) {
    	return f.tree(name);
    }
    
    @Override
    public IValue make(IValueFactory f, String name, IValue... children) {
    	return f.tree(name, children);
    }
}
