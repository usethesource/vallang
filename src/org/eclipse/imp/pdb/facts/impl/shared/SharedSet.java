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
package org.eclipse.imp.pdb.facts.impl.shared;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.Set;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashSet;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * Implementation of shareable sets.
 * 
 * @author Arnold Lankamp
 */
public class SharedSet extends Set implements IShareable{
	
	protected SharedSet(Type elementType, ShareableValuesHashSet data){
		super(elementType, data);
	}
	
	protected SharedSet(Type subTypeOfSet, Type elementType, ShareableValuesHashSet data){
		super(subTypeOfSet, elementType, data);
	}
	
	public ISet insert(IValue value){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		newData.add(value);

		Type newElementType = elementType.lub(value.getType());
		return SharedValueFactory.getInstance().createSetWriter(newElementType, newData).done();
	}
	
	public ISet delete(IValue value){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		newData.remove(value);
		
		return SharedValueFactory.getInstance().createSetWriter(elementType, newData).done();
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

		Type newElementType = elementType.lub(other.getElementType());
		return SharedValueFactory.getInstance().createSetWriter(newElementType, commonData).done();
	}
	
	public ISet subtract(ISet other){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		
		Iterator<IValue> setIterator = other.iterator();
		while(setIterator.hasNext()){
			newData.remove(setIterator.next());
		}
		
		return SharedValueFactory.getInstance().createSetWriter(elementType, newData).done();
	}
	
	public ISet union(ISet other){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		Iterator<IValue> setIterator;
		
		SharedSet otherSet = (SharedSet) other;
		
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
		return SharedValueFactory.getInstance().createSetWriter(newElementType, newData).done();
	}
	
	public IRelation product(ISet other){
		SharedValueFactory sharedValueFactory = SharedValueFactory.getInstance();
		
		ShareableValuesHashSet newData = new ShareableValuesHashSet();
		
		Type tupleType = typeFactory.tupleType(elementType, other.getElementType());

		Iterator<IValue> thisIterator = data.iterator();
		while(thisIterator.hasNext()){
			IValue left = thisIterator.next();
			
			Iterator<IValue> setIterator = other.iterator();
			while(setIterator.hasNext()){
				IValue right = setIterator.next();
				
				IValue[] tuple = new IValue[]{left, right};
				newData.add(sharedValueFactory.createTupleUnsafe(tuple));
			}
		}
		
		return sharedValueFactory.createRelationWriter(tupleType, newData).done();
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
