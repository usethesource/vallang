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

package org.rascalmpl.value.type;

import java.util.Iterator;
import java.util.Map;

import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.IList;
import org.rascalmpl.value.IListWriter;
import org.rascalmpl.value.IString;
import org.rascalmpl.value.IValueFactory;
import org.rascalmpl.value.exceptions.FactTypeUseException;

/*package*/ class ListType extends DefaultSubtypeOfValue {
	static final Type listConstructor = TF.constructor(symbolStore, Symbol, "list", Symbol, "symbol");
  
  protected final Type fEltType;
	
	/*package*/ ListType(Type eltType) {
		fEltType = eltType;
	}

	@Override
	protected Type getReifiedConstructorType() {
		return listConstructor;
	}
	
	public static Type fromSymbol(IConstructor symbol, TypeStore store) {
		return TF.listType(Type.fromSymbol((IConstructor) symbol.get("symbol")));
	}
	
	@Override
	public IConstructor asSymbol(IValueFactory vf) {
		return vf.constructor(listConstructor, fEltType.asSymbol(vf));
	}
	 
	@Override
	public Type getElementType() {
		return fEltType;
	}
	
	@Override
	public boolean hasFieldNames() {
		return fEltType.hasFieldNames();
	}
	
	@Override 
	public boolean hasField(String fieldName) {
		return fEltType.hasField(fieldName);
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
	public Type select(int... fields) {
		return TF.listType(fEltType.select(fields));
	}
	
	@Override
	public Type select(String... names) {
		return TF.listType(fEltType.select(names));
	}
	
	@Override
	public String toString() {
	  if (fEltType.isFixedWidth() && !fEltType.equivalent(VoidType.getInstance())) {
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
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
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
	  return other.lubWithList(this);
	}
	
	@Override
	public Type glb(Type type) {
	  return type.glbWithList(this);
	}
	
	@Override
	public Type lubWithList(Type type) {
		return this == type ? this : TF.listType(fEltType.lub(type.getElementType()));
	}
	
	@Override
	protected Type glbWithList(Type type) {
	  return this == type ? this : TF.listType(fEltType.glb(type.getElementType()));
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
