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

/**
 * A type for values that are nodes. All INode have the type NodeType, 
 * and all IConstructors have NodeType as a supertype.
 */
/*package*/ final class NodeType extends Type {
	private static class InstanceHolder {
		public static final NodeType sInstance= new NodeType();
	}
	
    public static NodeType getInstance() {
        return InstanceHolder.sInstance;
    }

    private NodeType() { }

    @Override
    public boolean isNodeType() {
    	return true;
    }
    
    @Override
    public String toString() {
        return "tree";
    }
    
    /**
     * Should never be called, NodeType is a singleton 
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof NodeType);
    }
    
    @Override
    public int hashCode() {
    	return 20102;
    }
    
    @Override
    public <T> T accept(ITypeVisitor<T> visitor) {
    	return visitor.visitNode(this);
    }
    
    @Override
    public IValue make(IValueFactory f, String name) {
    	return f.node(name);
    }
    
    @Override
    public IValue make(IValueFactory f, String name, IValue... children) {
    	return f.node(name, children);
    }
}
