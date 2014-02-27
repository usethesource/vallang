/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    jurgen@vinju.org

*******************************************************************************/
package org.eclipse.imp.pdb.facts.type;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UndeclaredAnnotationException;
import org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableMap;
import org.eclipse.imp.pdb.facts.util.ImmutableMap;

/**
 * A tree type is a type of tree node, defined by its name, the types of
 * its children and the type it produces. Example tree types would be:
 * 
 * Address ::= dutchAddress(Street, City, Postcode)
 * Address ::= usAddress(Street, City, State, PostalCode)
 * 
 * Here Address is the AbstractDataType, the type a tree produces. dutchAddress
 * and usAddress are the names of the node types and the other capitalized names
 * are the types of the children.
 * 
 * Children types can also be named as in:
 * Boolean ::= and(Boolean lhs, Boolean rhs)
 * Boolean ::= or(Boolean lhs, Boolean rhs)
 * 
 */
/*package*/ final class ConstructorType extends AbstractDataType {
	private final Type fChildrenTypes;
	private final Type fADT;
	private final String fName;
	private final ImmutableMap<String, Type> fKeywordParameters;
  private final ImmutableMap<String, IValue> fKeywordParameterDefaults;
	
	/* package */ ConstructorType(String name, Type childrenTypes, Type adt) {
	  super(adt.getName(), adt.getTypeParameters());
		fName = name.intern();
		fChildrenTypes = childrenTypes;
		fADT = adt;
		fKeywordParameterDefaults = null;
		fKeywordParameters = null;
	}
	
	/* package */ ConstructorType(String name, Type childrenTypes, Type adt, Map<String,Type> keywordParameters, Map<String,IValue> defaults) {
	  super(adt.getName(), adt.getTypeParameters());
    fName = name.intern();
    fChildrenTypes = childrenTypes;
    fADT = adt;

    fKeywordParameters = AbstractSpecialisedImmutableMap.mapOf(keywordParameters);
    fKeywordParameterDefaults = AbstractSpecialisedImmutableMap.mapOf(defaults);
	}

  @Override
	public Type carrier() {
		return fChildrenTypes.carrier();
	}

	@Override
	public int hashCode() {
		return 21 + 44927 * ((fName != null) ? fName.hashCode() : 1) + 
		181 * fChildrenTypes.hashCode() + 
		(fKeywordParameters == null ? 0 : 19 * fKeywordParameters.hashCode()) +
		354767453 * fADT.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ConstructorType) {
			ConstructorType other = (ConstructorType) o;
      return ((fName == null) ? other.fName == null : fName == (other.fName)) // fName is interned.
					&& fChildrenTypes == other.fChildrenTypes
					&& fKeywordParameterDefaults == other.fKeywordParameterDefaults
					&& fADT == other.fADT;
		}
		return false;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(fADT);
		builder.append(" = ");
		builder.append(fName);
		builder.append("(");

		Iterator<Type> iter = fChildrenTypes.iterator();
		while(iter.hasNext()) {
			builder.append(iter.next());

			if (iter.hasNext()) {
				builder.append(",");
			}
		}
		builder.append(")");

		return builder.toString();
	}
	
	@Override
	public int getArity() {
		return fChildrenTypes.getArity();
	}
	
	@Override
	public int getFieldIndex(String fieldName) throws FactTypeUseException {
		return fChildrenTypes.getFieldIndex(fieldName);
	}
	
	@Override
	public boolean hasField(String fieldName) {
		return fChildrenTypes.hasField(fieldName);
	}
	
	@Override
	public boolean hasField(String fieldName, TypeStore store) {
		return hasField(fieldName);
	}
	
	@Override
	public Type getFieldTypes() {
		return fChildrenTypes;
	}
	

	@Override
	public String getName() {
		return fName;
	}
	
	@Override
	public Type getAbstractDataType() {
		return fADT;
	}
	
	@Override
	public Type getFieldType(int i) {
		return fChildrenTypes.getFieldType(i);
	}
	
	@Override
	public Type getFieldType(String fieldName) throws FactTypeUseException {
		return fChildrenTypes.getFieldType(fieldName);
	}
	
	@Override
	public String getFieldName(int i) {
		return fChildrenTypes.getFieldName(i);
	}
	
	@Override
	public String[] getFieldNames() {
	  return fChildrenTypes.getFieldNames();
	}
	
	@Override
	public boolean hasFieldNames() {
		return fChildrenTypes.hasFieldNames();
	}
	
	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitConstructor(this);
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
	  return type.isSubtypeOfConstructor(this);
	}
	
	@Override
	public Type lub(Type type) {
	  return type.lubWithConstructor(this);
	}
	
	@Override
	public Type glb(Type type) {
	  return type.glbWithConstructor(this);
	}
	
	@Override
	protected boolean isSubtypeOfConstructor(Type type) {
	  if (type.getName().equals(getName())) {
      return getAbstractDataType().isSubtypeOf(type.getAbstractDataType())
          && getFieldTypes().isSubtypeOf(type.getFieldTypes());
    }
    else {
      return false;
    }
	}
	
	@Override
	protected boolean isSubtypeOfAbstractData(Type type) {
	  return getAbstractDataType().isSubtypeOfAbstractData(type);
	}
	
	@Override
	protected Type lubWithConstructor(Type type) {
		if(this == type) {
			return this;
		}
		return getAbstractDataType().lubWithAbstractData(type.getAbstractDataType());
	}
	
	@Override
	protected Type glbWithConstructor(Type type) {
	  if (type.isSubtypeOf(this)) {
	    return type;
	  }
	  else if (isSubtypeOf(type)) {
	    return this;
	  }
	  else {
	    return TF.voidType();
	  }
	}
	
	@Override
	protected Type glbWithAbstractData(Type type) {
	  if (isSubtypeOf(type)) {
	    return this;
	  }
	  
	  return TF.voidType();
	}
	
	@Override
	protected Type glbWithNode(Type type) {
	  return this;
	}

	@Override
	public boolean match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		return super.match(matched, bindings)
				&& fADT.match(matched.getAbstractDataType(), bindings)
				&& getFieldTypes().match(matched.getFieldTypes(), bindings);
	}
	
	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		if (bindings.isEmpty()) {
			return this;
		}
		Type adt = fADT.instantiate(bindings);
		Type fields = getFieldTypes().instantiate(bindings);
		
		TypeStore store = new TypeStore();
		store.declareAbstractDataType(fADT);
		store.declareConstructor(this);
		
		return TypeFactory.getInstance().constructorFromTuple(store, adt, getName(), fields);
	}
	
	@Override
	public boolean declaresAnnotation(TypeStore store, String label) {
		return store.getAnnotationType(this, label) != null;
	}
	
	@Override
	public Type getAnnotationType(TypeStore store, String label) throws FactTypeUseException {
		Type type = store.getAnnotationType(this, label);
		
		if (type == null) {
			throw new UndeclaredAnnotationException(getAbstractDataType(), label);
		}
		
		return type;
	}
	
	@Override
	public boolean isParameterized() {
		return fADT.isParameterized();
	}
	
	@Override
	public boolean hasKeywordParameters(){
		return fKeywordParameters != null;
	}
	
	@Override
	public IValue getKeywordParameterDefault(String label) {
	  return fKeywordParameterDefaults != null ? fKeywordParameterDefaults.get(label) : null;
	}
	
	@Override
	public Type getKeywordParameterType(String label) {
	  return fKeywordParameters != null ? fKeywordParameters.get(label) : null;
	}
	
	@Override
	public Set<String> getKeywordParameters() {
	  return fKeywordParameters.keySet();
	}
}
