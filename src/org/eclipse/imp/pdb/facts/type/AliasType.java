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

import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.IWriter;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

/**
 * A AliasType is a named for a type, i.e. a type alias that can be
 * used to abstract from types that have a complex structure. Since a 
 * @{link Type} represents a set of values, a AliasType is defined to 
 * be an alias (because it represents the same set of values).
 * <p>
 * Detail: This API does not allow @{link IValue}'s of the basic types (int, 
 * double, etc) to be tagged with a AliasType. Instead the immediately
 * surrounding structure, such as a set or a relation will refer to the
 * AliasType.
 */
/*package*/ final class AliasType extends Type {
	private final String fName;
	private final Type fAliased;
	private final Type fParameters;
	
	/* package */ AliasType(String name, Type aliased) {
		fName = name;
		fAliased = aliased;
		fParameters = TypeFactory.getInstance().voidType();
	}
	
	/* package */ AliasType(String name, Type aliased, Type parameters) {
		fName = name;
		fAliased = aliased;
		fParameters = parameters;
	}
	
	@Override
	public boolean isAliasType() {
		return true;
	}
	
	@Override
	public boolean isParameterized() {
		return !fParameters.isVoidType();
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
	
	/**
	 * @return the first super type of this type that is not a AliasType.
	 */
	@Override
	public Type getHiddenType() {
		return fAliased;
	}

	@Override
	public boolean isSubtypeOf(Type other) {
		if (other == this) {
			return true;
		}
		else if (other.isAliasType() && other.getName().equals(getName())) {
			return fAliased.isSubtypeOf(other) && fParameters.isSubtypeOf(other.getTypeParameters());
		}
		else {
			return fAliased.isSubtypeOf(other);
		}
	}

	@Override
	public Type lub(Type other) {
		if (other == this) {
			return this;
		}
		else if (other.isAliasType() && other.getName().equals(getName())) {
			Type aliased = fAliased.lub(other.getAliased());
			Type params = fParameters.lub(other.getTypeParameters());
			return TypeFactory.getInstance().aliasTypeFromTuple(new TypeStore(), getName(), aliased, params);
		}
		else {
			return fAliased.lub(other);
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(fName);
		if (!fParameters.isVoidType()) {
			sb.append("[");
			int idx= 0;
			for(Type elemType: fParameters) {
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
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitAlias(this);
	}
	
	@Override
	public IInteger make(IValueFactory f, int arg) {
		return (IInteger) fAliased.make(f, arg);
	}
	
	@Override
	public IValue make(IValueFactory f) {
		return fAliased.make(f);
	}
	
	@Override
	public IValue make(IValueFactory f, IValue... children) {
		return fAliased.make(f, children);
	}
	
	@Override
	public IValue make(IValueFactory f, URI url, int startOffset, int length, int startLine, int endLine, int startCol, int endCol) {
		return fAliased.make(f, url, startOffset, length, startLine, endLine, startCol, endCol);
	}
	
	@Override 
	public IValue make(IValueFactory f, String path, int startOffset, int length,
			int startLine, int endLine, int startCol, int endCol) {
		return f.sourceLocation(path, startOffset, length, startLine, endLine, startCol, endCol);
	}

	@Override 
    public IValue make(IValueFactory f, URI uri) {
    	return f.sourceLocation(uri);
    }
	
	@Override
	public IValue make(IValueFactory f, double arg) {
		return fAliased.make(f, arg);
	}
	
	@Override
	public IValue make(IValueFactory f, String arg) {
		return fAliased.make(f, arg);
	}
	
	@Override
	public <T extends IWriter> T writer(IValueFactory f) {
	    // RMF Feb 9, 2009: Work around a javac bug (see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954)
	    // that causes an erroneous compile error.
	    // Use this method's type parameter as an explicit qualifier on the method call type parameter.
	    // N.B.: The JDT doesn't exhibit this bug (at least not as of 3.3.2), so this fix is only
	    // relevant to the release build scripts.
		return fAliased.<T>writer(f);
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
	public boolean comparable(Type other) {
		return fAliased.comparable(other);
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
	public void match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		super.match(matched, bindings);
		fAliased.match(matched, bindings);
	}

	@Override
	public boolean isBoolType() {
		return fAliased.isBoolType();
	}

	@Override
	public boolean isRealType() {
		return fAliased.isRealType();
	}

	@Override
	public boolean isIntegerType() {
		return fAliased.isIntegerType();
	}

	@Override
	public boolean isListType() {
		return fAliased.isListType();
	}

	@Override
	public boolean isMapType() {
		return fAliased.isMapType();
	}

	@Override
	public boolean isAbstractDataType() {
		return fAliased.isAbstractDataType();
	}

	@Override
	public boolean isParameterType() {
		return fAliased.isParameterType();
	}

	@Override
	public boolean isRelationType() {
		return fAliased.isRelationType();
	}

	@Override
	public boolean isSetType() {
		return fAliased.isSetType();
	}

	@Override
	public boolean isSourceLocationType() {
		return fAliased.isSourceLocationType();
	}

	@Override
	public boolean isSourceRangeType() {
		return fAliased.isSourceRangeType();
	}

	@Override
	public boolean isStringType() {
		return fAliased.isStringType();
	}

	@Override
	public boolean isConstructorType() {
		return fAliased.isConstructorType();
	}

	@Override
	public boolean isNodeType() {
		return fAliased.isNodeType();
	}

	@Override
	public boolean isTupleType() {
		return fAliased.isTupleType();
	}

	@Override
	public boolean isValueType() {
		return fAliased.isValueType();
	}

	@Override
	public boolean isVoidType() {
		return fAliased.isVoidType();
	}

	@Override
	public boolean isExternalType(){
		return fAliased.isExternalType();
	}

	@Override
	public Iterator<Type> iterator() {
		return fAliased.iterator();
	}

	@Override
	public IValue make(IValueFactory f, boolean arg) {
		return fAliased.make(f, arg);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, boolean arg) {
		return fAliased.make(f, store, arg);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, double arg) {
		return fAliased.make(f, store, arg);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, int arg) {
		return fAliased.make(f, store, arg);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, String arg) {
		return fAliased.make(f, store, arg);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, String name, IValue... children) {
		return fAliased.make(f, store, name, children);
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
	public boolean hasField(String fieldName){
		return fAliased.hasField(fieldName);
	}

	@Override
	public boolean hasField(String fieldName, TypeStore store){
		return fAliased.hasField(fieldName, store);
	}
}
