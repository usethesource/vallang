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

import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

/*package*/ class ListType extends Type {
	protected static class Subtype extends ValueSubtype {
	  private final Type fEltType;
	  
    public Subtype(Type eltType) {
	    this.fEltType = eltType;
    }
    
    @Override
    public ValueSubtype visitList(Type type) {
      setSubtype(fEltType.isSubtypeOf(type.getElementType()));
      setLub(TF.listType(fEltType.lub(type.getElementType())));
      return this;
    }
    
    @Override
    public ValueSubtype visitListRelationType(Type type) {
      return visitList(type);
    }
  }

  protected final Type fEltType;
	
	/*package*/ ListType(Type eltType) {
		fEltType = eltType;
	}

	@Override
	public Type getElementType() {
		return fEltType;
	}
	
	@Override
	public boolean isListType() {
		return true;
	}
	
	@Override
	protected ValueSubtype getSubtype() {
	  return new Subtype(fEltType);
	}
	
	@Override
	public Type carrier() {
		return TypeFactory.getInstance().setType(fEltType);
	}
	
	@Override
	public String toString() {
		return "list[" + fEltType + "]";
	}

	@Override
	public boolean equals(Object o) {
		if(o == this) {
			return true;
		}
		else if (o instanceof ListType) {
			ListType other = (ListType) o;
			return fEltType == other.fEltType;
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return 75703 + 104543 * fEltType.hashCode();
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitList(this);
	}

	@Override
	public IValue make(IValueFactory f) {
		return f.list(fEltType);
	}

	@Override
	public IValue make(IValueFactory f, IValue... elems) {
		return f.list(elems);
	}

	@SuppressWarnings("unchecked")
	@Override
	public IListWriter writer(IValueFactory f) {
		return f.listWriter(fEltType);
	}
	
	@Override
	public boolean match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		return super.match(matched, bindings)
				&& getElementType().match(matched.getElementType(), bindings);
	}
	
	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		return TypeFactory.getInstance().listType(getElementType().instantiate(bindings));
	}
}
