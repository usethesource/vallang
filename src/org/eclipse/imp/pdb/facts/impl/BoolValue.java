/*******************************************************************************
* Copyright (c) 2012 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Jurgen Vinju
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

public abstract class BoolValue extends Value implements IBool{
	public final static BoolValue TRUE = new BoolValue() {
	  public boolean getValue() {
	    return true;
	  };
	  
	  public IBool not() {
	    return FALSE;
	  };
	  
	  public IBool and(IBool other) {
	    return other;
	  };
	  
	  public IBool or(IBool other) {
	    return this;
	  };
	  
	  public IBool xor(IBool other) {
	    return other == this ? FALSE : TRUE;
	  };
	  
	  public IBool implies(IBool other) {
	    return other;
	  };
	  
	  public int hashCode() {
	    return 1;
	  };
	};
	
	public final static BoolValue FALSE = new BoolValue() {
	  public boolean getValue() {
	    return false;
	  };
	  
	  public IBool not() {
	    return TRUE;
	  }

    public IBool and(IBool other) {
      return this;
    }

    public IBool or(IBool other) {
      return other;
    }

    public IBool xor(IBool other) {
      return other;
    }

    public IBool implies(IBool other) {
      return TRUE;
    }
    
    public int hashCode() {
	  return 2;
	};
	};
	
	private BoolValue() {
		super(TypeFactory.getInstance().boolType());
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitBoolean(this);
	}
	
	public static BoolValue getBoolValue(boolean bool){
		return bool ? TRUE : FALSE;
	}
	
	public abstract int hashCode();
	
	public boolean equals(Object o){
		return this == o;
	}
	
	public boolean isEqual(IValue value){
		return this == value;
	}
	
	public IBool equivalent(IBool other) {
    return other == this ? TRUE : this;
  };
	
	public String getStringRepresentation(){
		return toString();
	}
}
