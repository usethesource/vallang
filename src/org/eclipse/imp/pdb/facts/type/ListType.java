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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

/*package*/ class ListType extends ValueType {
  protected final Type fEltType;
	
	/*package*/ ListType(Type eltType) {
		fEltType = eltType;
	}

	@Override
	public Type getElementType() {
		return fEltType;
	}
	
	 @Override
	  public int getFieldIndex(String fieldName) {
	    return fEltType.getFieldIndex(fieldName);
	  }
	  
	  @Override
	  public Type getFieldType(int i) {
	    return fEltType.getFieldType(i);
	  }
	  
	  @Override
	  public String getFieldName(int i) {
	    return fEltType.getFieldName(i);
	  }
	  
	  @Override
	  public String[] getFieldNames() {
	    return fEltType.getFieldNames();
	  }
	  
	  @Override
	  public int getArity() {
	    return fEltType.getArity();
	  }
	  
	  @Override
	  public Type getFieldType(String fieldName) throws FactTypeUseException {
	    return fEltType.getFieldType(fieldName);
	  }
	  
	  @Override
	  public Type getFieldTypes() {
	    return fEltType.getFieldTypes();
	  }
	  
	@Override
	public Type carrier() {
		return fEltType.carrier();
	}
	
	@Override
	public Type closure() {
	  return TF.listType(fEltType.closure());
	}
	
	@Override
	public Type compose(Type other) {
	  return TF.listType(fEltType.compose(other.getElementType()));
	}
	
	@Override
	public String toString() {
	  if (fEltType.isFixedWidth()) {
	        StringBuilder sb = new StringBuilder();
	    sb.append("lrel[");
	    int idx = 0;
	    Iterator<Type> iter = fEltType.iterator();
	    while(iter.hasNext()) {
	      Type elemType = iter.next();
	      if (idx++ > 0)
	        sb.append(",");
	      sb.append(elemType.toString());
	      if (hasFieldNames()) {
	        sb.append(" " + fEltType.getFieldName(idx - 1));
	      }
	    }
	    sb.append("]");
	    return sb.toString();
	  }
	  else {
	    return "list[" + fEltType + "]";
	  }
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
	public <T,E extends Exception> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitList(this);
	}
	
	@Override
	public boolean isOpen() {
	  return fEltType.isOpen();
	}
	
	@Override
	protected boolean isSupertypeOf(Type type) {
	  return type.isSubtypeOfList(this);
	}
	
	@Override 
	protected boolean isSubtypeOfList(Type type) {
		return fEltType.isSubtypeOf(type.getElementType());
	}
	
	@Override
	public Type lub(Type other) {
	  return other.lubWithSet(this);
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
