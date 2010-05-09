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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of IConstructor.
 * <br /><br />
 * Constructors that are annotated will use the AnnotatedConstructor class instead.
 * 
 * @author Arnold Lankamp
 */
public class Constructor extends Value implements IConstructor{
	protected final Type constructorType;
	protected final IValue[] children;

	protected Constructor(Type constructorType, IValue[] children){
		super();
		
		this.constructorType = constructorType;
		
		this.children = children;
	}
	
	public Type getType(){
		return constructorType.getAbstractDataType();
	}
	
	public Type getConstructorType(){
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
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
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
		return new ShareableHashMap<String, IValue>();
	}
	
	public IConstructor setAnnotation(String label, IValue value){
		return new AnnotatedConstructor(constructorType, children, getUpdatedAnnotations(label, value));
	}
	
	public IConstructor setAnnotations(Map<String, IValue> newAnnos){
		if(newAnnos.isEmpty()) return this;
	
		return new AnnotatedConstructor(constructorType, children, getSetAnnotations(newAnnos));
	}
	
	public IConstructor joinAnnotations(Map<String, IValue> newAnnos){
		return new AnnotatedConstructor(constructorType, children, getUpdatedAnnotations(newAnnos));
	}
	
	public IConstructor removeAnnotation(String label){
		return new AnnotatedConstructor(constructorType, children, getUpdatedAnnotations(label));
	}
	
	public IConstructor removeAnnotations(){
		return this;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label, IValue value){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>();
		newAnnotations.put(label, value);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>();
		newAnnotations.remove(label);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(Map<String, IValue> newAnnos){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>();
		
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
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>();
		
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
		
		if(value instanceof Constructor){
			Constructor otherTree = (Constructor) value;
			
			if(!constructorType.comparable(otherTree.constructorType)) return false;
			
			IValue[] otherChildren = otherTree.children;
			int nrOfChildren = children.length;
			if(otherChildren.length == nrOfChildren){
				for(int i = nrOfChildren - 1; i >= 0; i--){
					if(!otherChildren[i].isEqual(children[i])) return false;
				}
				return true;
			}
		}
		
		return false;
	}
	
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
}
