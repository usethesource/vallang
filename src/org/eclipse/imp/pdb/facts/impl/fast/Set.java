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

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashSet;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of ISet.
 * 
 * @author Arnold Lankamp
 */
public class Set implements ISet{
	protected final static TypeFactory typeFactory = TypeFactory.getInstance();
	protected final static Type voidType = typeFactory.voidType();
	
	protected final Type setType;
	protected final Type elementType;
	
	protected final ShareableValuesHashSet data;
	
	protected Set(Type elementType, ShareableValuesHashSet data){
		super();
		
		this.setType = typeFactory.setType(elementType);
		this.elementType = elementType;
		
		this.data = data;
	}
	
	protected Set(Type subTypeOfSet, Type elementType, ShareableValuesHashSet data){
		super();
		
		this.setType = subTypeOfSet;
		this.elementType = elementType;
		
		this.data = data;
	}

	public Type getType(){
		return setType;
	}
	
	public Type getElementType(){
		return elementType;
	}

	public int size(){
		return data.size();
	}
	
	public boolean isEmpty(){
		return data.isEmpty();
	}
	
	public Iterator<IValue> iterator(){
		return data.iterator();
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitSet(this);
	}
	
	public boolean contains(IValue element){
		return data.contains(element);
	}
	
	public boolean isSubsetOf(ISet other){
		Set otherSet = (Set) other;
		
		Iterator<IValue> iterator = iterator();
		while(iterator.hasNext()){
			if(!otherSet.data.contains(iterator.next())) return false;
		}
		
		return true;
	}
	
	public ISet insert(IValue value){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		newData.add(value);

		Type type = elementType.lub(value.getType());
		return createSetWriter(type, newData).done();
	}

	public ISet delete(IValue value){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		newData.remove(value);
		
		return createSetWriter(elementType, newData).done();
	}
	
	public ISet intersect(ISet other){
		ShareableValuesHashSet commonData = new ShareableValuesHashSet();
		Iterator<IValue> setIterator;
		
		ISet theOtherSet;
		
		if(other.size() < size()){
			setIterator = other.iterator();
			theOtherSet = this;
		}else{
			setIterator = iterator();
			theOtherSet = other;
		}
		
		while(setIterator.hasNext()){
			IValue value = setIterator.next();
			if(theOtherSet.contains(value)){
				commonData.add(value);
			}
		}
		
		Type type = elementType.lub(other.getElementType());
		return createSetWriter(type, commonData).done();
	}
	
	public ISet subtract(ISet other){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		
		Iterator<IValue> setIterator = other.iterator();
		while(setIterator.hasNext()){
			newData.remove(setIterator.next());
		}
		
		return createSetWriter(elementType, newData).done();
	}
	
	public ISet union(ISet other){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		Iterator<IValue> setIterator;
		
		Set otherSet = (Set) other;
		
		if(otherSet.size() < size()){
			newData = new ShareableValuesHashSet(data);
			setIterator = otherSet.iterator();
		}else{
			newData = new ShareableValuesHashSet(otherSet.data);
			setIterator = iterator();
		}
		
		while(setIterator.hasNext()){
			newData.add(setIterator.next());
		}
		
		Type newElementType = elementType.lub(otherSet.elementType);
		return createSetWriter(newElementType, newData).done();
	}
	
	public IRelation product(ISet other){
		ShareableValuesHashSet newData = new ShareableValuesHashSet();
		
		Type tupleType = typeFactory.tupleType(elementType, other.getElementType());

		Iterator<IValue> thisIterator = data.iterator();
		while(thisIterator.hasNext()){
			IValue left = thisIterator.next();
			
			Iterator<IValue> setIterator = other.iterator();
			while(setIterator.hasNext()){
				IValue right = setIterator.next();
				
				IValue[] tuple = new IValue[]{left, right};
				newData.add(new Tuple(tupleType, tuple));
			}
		}
		
		return new RelationWriter(tupleType, newData).done();
	}
	
	public int hashCode(){
		return data.hashCode();
	}
	
	public boolean equals(Object o){
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			Set otherSet = (Set) o;
			
			if(setType != otherSet.setType) return false;
			
			return data.equals(otherSet.data);
		}
		
		return false;
	}
	
	public boolean isEqual(IValue value){
		if(value == this) return true;
		if(value == null) return false;
		
		if(value instanceof Set){
			Set otherSet = (Set) value;
			
			if(!elementType.comparable(otherSet.elementType)) return false;
			
			return data.isEqual(otherSet.data);
		}
		
		return false;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("{");
		
		Iterator<IValue> setIterator = iterator();
		if(setIterator.hasNext()){
			sb.append(setIterator.next());
			
			while(setIterator.hasNext()){
				sb.append(",");
				sb.append(setIterator.next());
			}
		}
		
		sb.append("}");
		
		return sb.toString();
	}
	
	protected static ISetWriter createSetWriter(Type elementType, ShareableValuesHashSet data){
		if(elementType.isTupleType()) return new RelationWriter(elementType, data);
		
		return new SetWriter(elementType, data);
	}
}
