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
import java.util.Map.Entry;

import org.eclipse.imp.pdb.facts.IAnnotatable;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.AbstractDefaultAnnotatable;
import org.eclipse.imp.pdb.facts.impl.AbstractValue;
import org.eclipse.imp.pdb.facts.impl.AnnotatedConstructorFacade;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.util.ArrayIterator;
import org.eclipse.imp.pdb.facts.util.ImmutableMap;
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

	/*package*/ static IConstructor newConstructor(Type constructorType, IValue[] children) {
		return new Constructor(constructorType, children); 
	}
	
	/*package*/ static IConstructor newConstructor(Type constructorType, IValue[] children, Map<String,IValue> kwParams) {
	  IValue[] allChildren = new IValue[children.length + kwParams.size()];
	  System.arraycopy(children, 0, allChildren, 0, children.length);
	  
	  for (Entry<String,IValue> entry : kwParams.entrySet()) {
	    allChildren[constructorType.getFieldIndex(entry.getKey())] = entry.getValue();
	  }

	  return new Constructor(constructorType, allChildren);
	}
	
	private Constructor(Type constructorType, IValue[] children){
		super();
		
		this.constructorType = constructorType;
		this.children = children;
	}
	
	@Override
	public Type getUninstantiatedConstructorType() {
	  return constructorType;
	}
	
	@Override
	public Type getType(){
		return getConstructorType().getAbstractDataType();
	}
	
	@Override
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
	
	@Override
	public Type getChildrenTypes(){
		return constructorType.getFieldTypes();
	}

	@Override
	public String getName(){
		return constructorType.getName();
	}
	
	@Override
	public int arity(){
		return children.length;
	}

	@Override
	public IValue get(int i){
		return children[i];
	}
	
	@Override
	public IValue get(String label){
		return get(constructorType.getFieldIndex(label));
	}

	@Override
	public Iterable<IValue> getChildren(){
		return this;
	}

	@Override
	public Iterator<IValue> iterator(){
		return ArrayIterator.of(children);
	}
	
	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E{
		return v.visitConstructor(this);
	}
	
	@Override
	public IConstructor set(int i, IValue newChild){
		IValue[] newChildren = children.clone();
		newChildren[i] = newChild;
		
		return newConstructor(constructorType, newChildren);
	}
	
	@Override
	public IConstructor set(String label, IValue newChild){
		IValue[] newChildren = children.clone();
		newChildren[constructorType.getFieldIndex(label)] = newChild;
		
		return newConstructor(constructorType, newChildren);
	}
	
	@Override
	public boolean declaresAnnotation(TypeStore store, String label) {
		return (store.getAnnotationType(constructorType.getAbstractDataType(), label) != null);
	}
	
	@Override
	public int hashCode(){
		int hash = constructorType.hashCode();
		
		for(int i = children.length - 1; i >= 0; i--){
			hash = (hash << 23) + (hash >> 5);
			hash ^= children[i].hashCode();
		}
		
		return hash;
	}
	
	@Override
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
	
	@Override
	public boolean isEqual(IValue value){
		if(value == this) return true;
		if(value == null) return false;
		
		if(value instanceof IConstructor){
			IConstructor otherTree = (IConstructor) value;
			
			if(!constructorType.comparable(otherTree.getConstructorType())) {
			  return false;
			}
			
			final Iterator<IValue> it1 = this.iterator();
			final Iterator<IValue> it2 = otherTree.iterator();

			while (it1.hasNext() && it2.hasNext()) {
				// call to IValue.isEqual(IValue)
				if (it1.next().isEqual(it2.next()) == false) {
					return false;
				}
			}

			// TODO: if keyword parameters are better supported with default values, 
			// this can become 'true' again.
			return (!it1.hasNext() && !it2.hasNext());
		}
		
		return false;
	}

	@Override
	public boolean has(String label) {
		return getConstructorType().hasField(label);
	}

	@Override
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
		return constructorType.hasKeywordArguments();
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
		return new AbstractDefaultAnnotatable<IConstructor>(this) {

			@Override
			protected IConstructor wrap(IConstructor content,
					ImmutableMap<String, IValue> annotations) {
				return new AnnotatedConstructorFacade(content, annotations);
			}
		};
	}
	
}
