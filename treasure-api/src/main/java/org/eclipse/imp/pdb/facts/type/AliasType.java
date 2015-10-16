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

/**
 * A AliasType is a named for a type, i.e. a type alias that can be used to
 * abstract from types that have a complex structure. Since a
 * 
 * @{link Type} represents a set of values, a AliasType is defined to be an
 *        alias (because it represents the same set of values).
 *        <p>
 *        Detail: This API does not allow @{link IValue}'s of the basic types
 *        (int, double, etc) to be tagged with a AliasType. Instead the
 *        immediately surrounding structure, such as a set or a relation will
 *        refer to the AliasType.
 */
/* package */ final class AliasType extends Type {
	private final String fName;
	private final Type fAliased;
	private final Type fParameters;

	/* package */ AliasType(String name, Type aliased) {
		fName = name.intern();
		fAliased = aliased;
		fParameters = TypeFactory.getInstance().voidType();
	}

	/* package */ AliasType(String name, Type aliased, Type parameters) {
		fName = name.intern();
		fAliased = aliased;
		fParameters = parameters;
	}

	@Override
	public boolean isParameterized() {
		return !fParameters.equivalent(VoidType.getInstance());
	}

	@Override
	public boolean isOpen() {
		return fParameters.isOpen();
	}

	@Override
	public boolean isAliased() {
		return true;
	}

	@Override
	public boolean isFixedWidth() {
		return fAliased.isFixedWidth();
	}

	/**
	 * @return the type parameters of the alias type, void when there are none
	 */
	@Override
	public Type getTypeParameters() {
		return fParameters;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public Type getAliased() {
		return fAliased;
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
		return type.isSubtypeOfAlias(this);
	}

	@Override
	public Type lub(Type other) {
		return other.lubWithAlias(this);
	}

	@Override
	public Type glb(Type type) {
		return type.glbWithAlias(this);
	}

	@Override
	protected Type lubWithAlias(Type type) {
		if (this == type)
			return this;
		if (getName().equals(type.getName())) {
			return TypeFactory.getInstance().aliasTypeFromTuple(new TypeStore(), type.getName(),
					getAliased().lub(type.getAliased()), getTypeParameters().lub(type.getTypeParameters()));
		}

		return getAliased().lub(type);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(fName);
		if (isParameterized()) {
			sb.append("[");
			int idx = 0;
			for (Type elemType : fParameters) {
				if (idx++ > 0) {
					sb.append(",");
				}
				sb.append(elemType.toString());
			}
			sb.append("]");
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		return 49991 + 49831 * fName.hashCode() + 67349 * fAliased.hashCode() + 1433 * fParameters.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof AliasType) {
			AliasType other = (AliasType) o;
			return fName.equals(other.fName) && fAliased == other.fAliased && fParameters == other.fParameters;
		}
		return false;
	}

	@Override
	public <T, E extends Throwable> T accept(ITypeVisitor<T, E> visitor) throws E {
		return visitor.visitAlias(this);
	}

	@Override
	public int getArity() {
		return fAliased.getArity();
	}

	@Override
	public Type getBound() {
		return fAliased.getBound();
	}

	@Override
	public Type getElementType() {
		return fAliased.getElementType();
	}

	@Override
	public int getFieldIndex(String fieldName) {
		return fAliased.getFieldIndex(fieldName);
	}

	@Override
	public String getFieldName(int i) {
		return fAliased.getFieldName(i);
	}

	@Override
	public String getKeyLabel() {
		return fAliased.getKeyLabel();
	}

	@Override
	public String getValueLabel() {
		return fAliased.getValueLabel();
	}

	@Override
	public Type getFieldType(int i) {
		return fAliased.getFieldType(i);
	}

	@Override
	public Type getFieldType(String fieldName) {
		return fAliased.getFieldType(fieldName);
	}

	@Override
	public Type getFieldTypes() {
		return fAliased.getFieldTypes();
	}

	@Override
	public Type getKeyType() {
		return fAliased.getKeyType();
	}

	@Override
	public Type getValueType() {
		return fAliased.getValueType();
	}

	@Override
	public Type compose(Type other) {
		return fAliased.compose(other);
	}

	@Override
	public boolean hasFieldNames() {
		return fAliased.hasFieldNames();
	}

	@Override
	public boolean hasKeywordField(String fieldName, TypeStore store) {
		return fAliased.hasKeywordField(fieldName, store);
	}	

	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		Type[] params = new Type[0];
		if (isParameterized()) {
			params = new Type[fParameters.getArity()];
			int i = 0;
			for (Type p : fParameters) {
				params[i++] = p.instantiate(bindings);
			}
		}

		TypeStore store = new TypeStore();
		store.declareAlias(this);

		return TypeFactory.getInstance().aliasType(store, fName, fAliased.instantiate(bindings), params);
	}

	@Override
	public boolean match(Type matched, Map<Type, Type> bindings) throws FactTypeUseException {
		return super.match(matched, bindings) && fAliased.match(matched, bindings);
	}

	@Override
	public Iterator<Type> iterator() {
		return fAliased.iterator();
	}

	@Override
	public Type select(int... fields) {
		return fAliased.select(fields);
	}

	@Override
	public Type select(String... names) {
		return fAliased.select(names);
	}

	@Override
	public boolean hasField(String fieldName) {
		return fAliased.hasField(fieldName);
	}

	@Override
	public boolean hasField(String fieldName, TypeStore store) {
		return fAliased.hasField(fieldName, store);
	}

	@Override
	protected boolean isSubtypeOfReal(Type type) {
		return fAliased.isSubtypeOfReal(type);
	}

	@Override
	protected boolean isSubtypeOfInteger(Type type) {
		return fAliased.isSubtypeOfInteger(type);
	}

	@Override
	protected boolean isSubtypeOfRational(Type type) {
		return fAliased.isSubtypeOfRational(type);
	}

	@Override
	protected boolean isSubtypeOfList(Type type) {
		return fAliased.isSubtypeOfList(type);
	}

	@Override
	protected boolean isSubtypeOfMap(Type type) {
		return fAliased.isSubtypeOfMap(type);
	}

	@Override
	protected boolean isSubtypeOfNumber(Type type) {
		return fAliased.isSubtypeOfNumber(type);
	}

	@Override
	protected boolean isSubtypeOfRelation(Type type) {
		return fAliased.isSubtypeOfRelation(type);
	}

	@Override
	protected boolean isSubtypeOfListRelation(Type type) {
		return fAliased.isSubtypeOfListRelation(type);
	}

	@Override
	protected boolean isSubtypeOfSet(Type type) {
		return fAliased.isSubtypeOfSet(type);
	}

	@Override
	protected boolean isSubtypeOfSourceLocation(Type type) {
		return fAliased.isSubtypeOfSourceLocation(type);
	}

	@Override
	protected boolean isSubtypeOfString(Type type) {
		return fAliased.isSubtypeOfString(type);
	}

	@Override
	protected boolean isSubtypeOfNode(Type type) {
		return fAliased.isSubtypeOfNode(type);
	}

	@Override
	protected boolean isSubtypeOfConstructor(Type type) {
		return fAliased.isSubtypeOfConstructor(type);
	}

	@Override
	protected boolean isSubtypeOfAbstractData(Type type) {
		return fAliased.isSubtypeOfAbstractData(type);
	}

	@Override
	protected boolean isSubtypeOfTuple(Type type) {
		return fAliased.isSubtypeOfTuple(type);
	}

	@Override
	protected boolean isSubtypeOfVoid(Type type) {
		return fAliased.isSubtypeOfVoid(type);
	}

	@Override
	protected boolean isSubtypeOfBool(Type type) {
		return fAliased.isSubtypeOfBool(type);
	}

	@Override
	protected boolean isSubtypeOfExternal(Type type) {
		return fAliased.isSubtypeOfExternal(type);
	}

	@Override
	protected boolean isSubtypeOfDateTime(Type type) {
		return fAliased.isSubtypeOfDateTime(type);
	}

	@Override
	protected Type lubWithReal(Type type) {
		return fAliased.lubWithReal(type);
	}

	@Override
	protected Type lubWithInteger(Type type) {
		return fAliased.lubWithInteger(type);
	}

	@Override
	protected Type lubWithRational(Type type) {
		return fAliased.lubWithRational(type);
	}

	@Override
	protected Type lubWithList(Type type) {
		return fAliased.lubWithList(type);
	}

	@Override
	protected Type lubWithMap(Type type) {
		return fAliased.lubWithMap(type);
	}

	@Override
	protected Type lubWithNumber(Type type) {
		return fAliased.lubWithNumber(type);
	}

	@Override
	protected Type lubWithSet(Type type) {
		return fAliased.lubWithSet(type);
	}

	@Override
	protected Type lubWithSourceLocation(Type type) {
		return fAliased.lubWithSourceLocation(type);
	}

	@Override
	protected Type lubWithString(Type type) {
		return fAliased.lubWithString(type);
	}

	@Override
	protected Type lubWithNode(Type type) {
		return fAliased.lubWithNode(type);
	}

	@Override
	protected Type lubWithConstructor(Type type) {
		return fAliased.lubWithConstructor(type);
	}

	@Override
	protected Type lubWithAbstractData(Type type) {
		return fAliased.lubWithAbstractData(type);
	}

	@Override
	protected Type lubWithTuple(Type type) {
		return fAliased.lubWithTuple(type);
	}

	@Override
	protected Type lubWithValue(Type type) {
		return fAliased.lubWithValue(type);
	}

	@Override
	protected Type lubWithVoid(Type type) {
		return fAliased.lubWithVoid(type);
	}

	@Override
	protected Type lubWithBool(Type type) {
		return fAliased.lubWithBool(type);
	}

	@Override
	protected Type lubWithExternal(Type type) {
		return fAliased.lubWithExternal(type);
	}

	@Override
	protected Type lubWithDateTime(Type type) {
		return fAliased.lubWithDateTime(type);
	}

	@Override
	protected boolean isSubtypeOfValue(Type type) {
		return true;
	}

	@Override
	protected Type glbWithReal(Type type) {
		return fAliased.glbWithReal(type);
	}

	@Override
	protected Type glbWithInteger(Type type) {
		return fAliased.glbWithInteger(type);
	}

	@Override
	protected Type glbWithRational(Type type) {
		return fAliased.glbWithRational(type);
	}

	@Override
	protected Type glbWithList(Type type) {
		return fAliased.glbWithList(type);
	}

	@Override
	protected Type glbWithMap(Type type) {
		return fAliased.glbWithMap(type);
	}

	@Override
	protected Type glbWithNumber(Type type) {
		return fAliased.glbWithNumber(type);
	}

	@Override
	protected Type glbWithSet(Type type) {
		return fAliased.glbWithSet(type);
	}

	@Override
	protected Type glbWithSourceLocation(Type type) {
		return fAliased.glbWithSourceLocation(type);
	}

	@Override
	protected Type glbWithString(Type type) {
		return fAliased.glbWithString(type);
	}

	@Override
	protected Type glbWithNode(Type type) {
		return fAliased.glbWithNode(type);
	}

	@Override
	protected Type glbWithConstructor(Type type) {
		return fAliased.glbWithConstructor(type);
	}

	@Override
	protected Type glbWithAlias(Type type) {
		if (this == type)
			return this;
		if (getName().equals(type.getName())) {
			return TypeFactory.getInstance().aliasTypeFromTuple(new TypeStore(), type.getName(),
					getAliased().glb(type.getAliased()), getTypeParameters().glb(type.getTypeParameters()));
		}

		return getAliased().glb(type);
	}

	@Override
	protected Type glbWithAbstractData(Type type) {
		return fAliased.glbWithAbstractData(type);
	}

	@Override
	protected Type glbWithTuple(Type type) {
		return fAliased.glbWithTuple(type);
	}

	@Override
	protected Type glbWithValue(Type type) {
		return fAliased.glbWithValue(type);
	}

	@Override
	protected Type glbWithVoid(Type type) {
		return fAliased.glbWithVoid(type);
	}

	@Override
	protected Type glbWithBool(Type type) {
		return fAliased.glbWithBool(type);
	}

	@Override
	protected Type glbWithExternal(Type type) {
		return fAliased.glbWithExternal(type);
	}

	@Override
	protected Type glbWithDateTime(Type type) {
		return fAliased.glbWithDateTime(type);
	}

	@Override
	public Type getAbstractDataType() {
		return fAliased.getAbstractDataType();
	}
}
