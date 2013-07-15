/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IAnnotatable;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.AbstractDefaultEmptyAnnotatable;
import org.eclipse.imp.pdb.facts.impl.AbstractValue;
import org.eclipse.imp.pdb.facts.impl.AnnotatedConstructorFacade;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

/**
 * Implementation of IConstructor.
 * <br /><br />
 * Constructors that are annotated will use the AnnotatedConstructor class instead.
 * 
 * @author Arnold Lankamp
 */
/*package*/ class Constructor extends AbstractValue implements IConstructor{
	protected final Type constructorType;
	protected final IValue[] children;

	/*package*/ Constructor(Type constructorType, IValue[] children){
		super();
		
		this.constructorType = constructorType;
		this.children = children;
	}
	
	@Override
	public Type getUninstantiatedConstructorType() {
	  return constructorType;
	}
	
	public Type getType(){
		return getConstructorType().getAbstractDataType();
	}
	
	public Type getConstructorType(){
	  if (constructorType.getAbstractDataType().isParameterized()) {
      // this assures we always have the most concrete type for constructors.
      Type[] actualTypes = new Type[children.length];
      for (int i = 0; i < children.length; i++) {
        actualTypes[i] = children[i].getType();
      }
    
      Map<Type,Type> bindings = new HashMap<Type,Type>();
      constructorType.getFieldTypes().match(TypeFactory.getInstance().tupleType(actualTypes), bindings);
      
      for (Type field : constructorType.getAbstractDataType().getTypeParameters()) {
        if (!bindings.containsKey(field)) {
          bindings.put(field, TypeFactory.getInstance().voidType());
        }
      }
      
      return constructorType.instantiate(bindings);
    }
	  
		return constructorType;
	}
	
	public Type getChildrenTypes(){
		return constructorType.getFieldTypes();
	}

	public String getName(){
		return constructorType.getName();
	}
	
	public int arity(){
		return children.length;
	}

	public IValue get(int i){
		return children[i];
	}
	
	public IValue get(String label){
		return get(constructorType.getFieldIndex(label));
	}

	public Iterable<IValue> getChildren(){
		return this;
	}

	public Iterator<IValue> iterator(){
		return new TreeIterator(this);
	}
	
	public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E{
		return v.visitConstructor(this);
	}
	
	public IConstructor set(int i, IValue newChild){
		IValue[] newChildren = children.clone();
		newChildren[i] = newChild;
		
		return new Constructor(constructorType, newChildren);
	}
	
	public IConstructor set(String label, IValue newChild){
		IValue[] newChildren = children.clone();
		newChildren[constructorType.getFieldIndex(label)] = newChild;
		
		return new Constructor(constructorType, newChildren);
	}
	
	public boolean declaresAnnotation(TypeStore store, String label) {
		return (store.getAnnotationType(constructorType.getAbstractDataType(), label) != null);
	}
	
	public boolean hasAnnotation(String label){
		return false;
	}
	
	public boolean hasAnnotations(){
		return false;
	}
	
	public IValue getAnnotation(String label){
		return null;
	}
	
	public Map<String, IValue> getAnnotations(){
		return new ShareableHashMap<>();
	}
	
	public IConstructor setAnnotation(String label, IValue value){
		return AnnotatedConstructor.createAnnotatedConstructor(constructorType, children, getUpdatedAnnotations(label, value));
	}
	
	public IConstructor setAnnotations(Map<String, IValue> newAnnos){
		if(newAnnos.isEmpty()) return this;
	
		return AnnotatedConstructor.createAnnotatedConstructor(constructorType, children, getSetAnnotations(newAnnos));
	}
	
	public IConstructor joinAnnotations(Map<String, IValue> newAnnos){
		return AnnotatedConstructor.createAnnotatedConstructor(constructorType, children, getUpdatedAnnotations(newAnnos));
	}
	
	public IConstructor removeAnnotation(String label){
		return AnnotatedConstructor.createAnnotatedConstructor(constructorType, children, getUpdatedAnnotations(label));
	}
	
	public IConstructor removeAnnotations(){
		return this;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label, IValue value){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		newAnnotations.put(label, value);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		newAnnotations.remove(label);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(Map<String, IValue> newAnnos){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		
		Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
		while(newAnnosIterator.hasNext()){
			Map.Entry<String, IValue> entry = newAnnosIterator.next();
			String key = entry.getKey();
			IValue value = entry.getValue();
			
			newAnnotations.put(key, value);
		}
		
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getSetAnnotations(Map<String, IValue> newAnnos){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		
		Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
		while(newAnnosIterator.hasNext()){
			Map.Entry<String, IValue> entry = newAnnosIterator.next();
			String key = entry.getKey();
			IValue value = entry.getValue();
			
			newAnnotations.put(key, value);
		}
		
		return newAnnotations;
	}
	
	public int hashCode(){
		int hash = constructorType.hashCode();
		
		for(int i = children.length - 1; i >= 0; i--){
			hash = (hash << 23) + (hash >> 5);
			hash ^= children[i].hashCode();
		}
		
		return hash;
	}
	
	public boolean equals(Object o){
		if(o == this) return true;
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			Constructor otherTree = (Constructor) o;
			
			if(constructorType != otherTree.constructorType) return false;
			
			IValue[] otherChildren = otherTree.children;
			int nrOfChildren = children.length;
			if(otherChildren.length == nrOfChildren){
				for(int i = nrOfChildren - 1; i >= 0; i--){
					if(!otherChildren[i].equals(children[i])) return false;
				}
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isEqual(IValue value){
		if(value == this) return true;
		if(value == null) return false;
		
		if(value instanceof IConstructor){
			IConstructor otherTree = (IConstructor) value;
			
			if(!constructorType.comparable(otherTree.getConstructorType())) return false;
			
			final Iterator<IValue> it1 = this.iterator();
			final Iterator<IValue> it2 = otherTree.iterator();

			while (it1.hasNext() && it2.hasNext()) {
				// call to IValue.isEqual(IValue)
				if (it1.next().isEqual(it2.next()) == false)
					return false;
			}

			assert (!it1.hasNext() && !it2.hasNext());
			return true;
		}
		
		return false;
	}
	
	/*
	 * TODO: replace by simple ArrayIterator. No need for Constructor specific
	 * iterator.
	 */
	private static class TreeIterator implements Iterator<IValue>{
		private final IValue[] children;
		private int index = 0;
		
		public TreeIterator(Constructor tree){
			super();
			
			children = tree.children;
		}
		
		public boolean hasNext(){
			return index < children.length;
		}
		
		public IValue next(){
			return children[index++];
		}
		
		public void remove(){
			throw new UnsupportedOperationException("This iterator doesn't support removal.");
		}
	}

	public boolean has(String label) {
		return getConstructorType().hasField(label);
	}

	public IConstructor replace(int first, int second, int end, IList repl)
			throws FactTypeUseException, IndexOutOfBoundsException {
		
		throw new UnsupportedOperationException("Replace not supported on constructor.");
	}
	
	@Override
	public IValue getKeywordArgumentValue(String name) {
		throw new UnsupportedOperationException("getKeyArgValue not supported on constructor.");
	}

	@Override
	public boolean hasKeywordArguments() {
		return false;
	}

	@Override
	public String[] getKeywordArgumentNames() {
		return constructorType.getFieldNames();
	}

	@Override
	public int getKeywordIndex(String name) {
		return constructorType.getFieldIndex(name);
	}

	@Override
	public int positionalArity() {
		return constructorType.getPositionalArity();
	}
	
	/**
	 * TODO: Create and move to {@link AbstractConstructor}.
	 */
	@Override
	public boolean isAnnotatable() {
		return true;
	}
	
	/**
	 * TODO: Create and move to {@link AbstractConstructor}.
	 */
	@Override
	public IAnnotatable<? extends IConstructor> asAnnotatable() {
		return new AbstractDefaultEmptyAnnotatable<IConstructor>(this) {

			@Override
			protected IConstructor wrap(IConstructor content,
					ShareableHashMap<String, IValue> annotations) {
				return new AnnotatedConstructorFacade(content, annotations);
			}
		};
	}
	
}
