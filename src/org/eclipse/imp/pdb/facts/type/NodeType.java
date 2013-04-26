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

import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

/**
 * A type for values that are nodes. All INode have the type NodeType, 
 * and all IConstructors have NodeType as a supertype.
 */
/*package*/ final class NodeType extends Type {
	protected static class Subtype extends ValueSubtype {
    @Override
    public ValueSubtype visitNode(Type type) {
      setSubtype(true);
      setLub(type);
      return null;
    }
    
    @Override
    public ValueSubtype visitAbstractData(Type type) {
      setSubtype(false);
      setLub(TF.nodeType());
      return null;
    }
    
    @Override
    public ValueSubtype visitConstructor(Type type) {
      setSubtype(false);
      setLub(TF.nodeType());
      return this;
    }
  }

  private final static NodeType sInstance= new NodeType();
	
    public static NodeType getInstance() {
        return sInstance;
    }

    private NodeType() {
    	super();
    }

    @Override
    public boolean isNodeType() {
    	return true;
    }
    
    @Override
    public String toString() {
        return "node";
    }
    
    @Override
    protected ValueSubtype getSubtype() {
      return new Subtype();
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
    
    public IValue make(IValueFactory f, TypeStore store, String name) {
    	return f.node(name);
	}
    
    @Override
    public IValue make(IValueFactory f, String name, IValue... children) {
    	return f.node(name, children);
    }
    
    @Override
    public IValue make(IValueFactory f, String name, IValue[] children, Map<String,IValue> keyArgValues) {
    	return f.node(name, children, keyArgValues);
    }
    
    @Override
    public IValue make(IValueFactory f, TypeStore store, String name, IValue... children) {
    	return make(f, name, children);
    }
}
