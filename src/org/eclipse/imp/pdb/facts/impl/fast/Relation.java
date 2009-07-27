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
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashSet;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.util.RotatingQueue;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of IRelation.
 * 
 * @author Arnold Lankamp
 */
public class Relation extends Set implements IRelation{
	
	protected Relation(Type tupleType, ShareableValuesHashSet data){
		super(typeFactory.relTypeFromTuple(tupleType), tupleType, data);
	}
	
	public Type getFieldTypes(){
		return elementType;
	}
	
	public int arity(){
		return elementType.getArity();
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitRelation(this);
	}
	
	public ISet insert(IValue value){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		newData.add(value);
		
		Type type = getElementType().lub(value.getType());
		return createSetWriter(type, newData).done();
	}

	public IRelation delete(IValue value){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		newData.remove(value);
		
		return new RelationWriter(elementType, newData).done();
	}
	
	public IRelation subtract(ISet set){
		ShareableValuesHashSet newData = new ShareableValuesHashSet(data);
		
		Iterator<IValue> setIterator = set.iterator();
		while(setIterator.hasNext()){
			newData.remove(setIterator.next());
		}
		
		return new RelationWriter(elementType, newData).done();
	}
	
	private ShareableValuesHashSet computeCarrier(){
		ShareableValuesHashSet newData = new ShareableValuesHashSet();
		
		Iterator<IValue> relationIterator = data.iterator();
		while(relationIterator.hasNext()){
			ITuple tuple = (ITuple) relationIterator.next();
			
			Iterator<IValue> tupleIterator = tuple.iterator();
			while(tupleIterator.hasNext()){
				newData.add(tupleIterator.next());
			}
		}
		
		return newData;
	}
	
	public ISet carrier(){
		ShareableValuesHashSet newData = computeCarrier();
		
		Type type = determainMostGenericTypeInTuple();
		return createSetWriter(type, newData).done();
	}
	
	public ISet domain(){
		ShareableValuesHashSet newData = new ShareableValuesHashSet();
		
		Iterator<IValue> relationIterator = data.iterator();
		while(relationIterator.hasNext()){
			ITuple tuple = (ITuple) relationIterator.next();
			
			newData.add(tuple.get(0));
		}
		
		Type type = elementType.getFieldType(0);
		return createSetWriter(type, newData).done();
	}
	
	public ISet range(){
		ShareableValuesHashSet newData = new ShareableValuesHashSet();
		
		int last = elementType.getArity() - 1;
		
		Iterator<IValue> relationIterator = data.iterator();
		while(relationIterator.hasNext()){
			ITuple tuple = (ITuple) relationIterator.next();
			
			newData.add(tuple.get(last));
		}
		
		Type type = elementType.getFieldType(last);
		return createSetWriter(type, newData).done();
	}
	
	public IRelation compose(IRelation other){
		Type otherTupleType = other.getFieldTypes();
		
		if(elementType == voidType) return this;
		if(otherTupleType == voidType) return other;
		
		if(elementType.getArity() != 2 || otherTupleType.getArity() != 2) throw new IllegalOperationException("compose", elementType, otherTupleType);
		if(!elementType.getFieldType(1).comparable(otherTupleType.getFieldType(0))) throw new IllegalOperationException("compose", elementType, otherTupleType);
		
		// Index
		ShareableHashMap<IValue, ShareableValuesList> rightSides = new ShareableHashMap<IValue, ShareableValuesList>();
		
		Iterator<IValue> otherRelationIterator = other.iterator();
		while(otherRelationIterator.hasNext()){
			ITuple tuple = (ITuple) otherRelationIterator.next();
			
			IValue key = tuple.get(0);
			ShareableValuesList values = rightSides.get(key);
			if(values == null){
				values = new ShareableValuesList();
				rightSides.put(key, values);
			}
			
			values.append(tuple.get(1));
		}
		
		// Compute
		ShareableValuesHashSet newData = new ShareableValuesHashSet();
		
		Type[] newTupleFieldTypes = new Type[]{elementType.getFieldType(0), otherTupleType.getFieldType(1)};
		Type tupleType = typeFactory.tupleType(newTupleFieldTypes);
		
		Iterator<IValue> relationIterator = data.iterator();
		while(relationIterator.hasNext()){
			ITuple thisTuple = (ITuple) relationIterator.next();
			
			IValue key = thisTuple.get(1);
			ShareableValuesList values = rightSides.get(key);
			if(values != null){
				Iterator<IValue> valuesIterator = values.iterator();
				do{
					IValue value = valuesIterator.next();
					IValue[] newTupleData = new IValue[]{thisTuple.get(0), value};
					newData.add(new Tuple(tupleType, newTupleData));
				}while(valuesIterator.hasNext());
			}
		}
		
		return new RelationWriter(tupleType, newData).done();
	}
	
	private ShareableValuesHashSet computeClosure(Type tupleType){
		ShareableValuesHashSet allData = new ShareableValuesHashSet(data);
		
		RotatingQueue<IValue> iLeftKeys = new RotatingQueue<IValue>();
		RotatingQueue<RotatingQueue<IValue>> iLefts = new RotatingQueue<RotatingQueue<IValue>>();
		
		ShareableHashMap<IValue, RotatingQueue<IValue>> interestingLeftSides = new ShareableHashMap<IValue, RotatingQueue<IValue>>();
		ShareableHashMap<IValue, ShareableValuesHashSet> potentialRightSides = new ShareableHashMap<IValue, ShareableValuesHashSet>();
		
		// Index
		Iterator<IValue> allDataIterator = allData.iterator();
		while(allDataIterator.hasNext()){
			ITuple tuple = (ITuple) allDataIterator.next();

			IValue key = tuple.get(0);
			IValue value = tuple.get(1);
			RotatingQueue<IValue> leftValues = interestingLeftSides.get(key);
			ShareableValuesHashSet rightValues;
			if(leftValues != null){
				rightValues = potentialRightSides.get(key);
			}else{
				leftValues = new RotatingQueue<IValue>();
				iLeftKeys.put(key);
				iLefts.put(leftValues);
				interestingLeftSides.put(key, leftValues);
				
				rightValues = new ShareableValuesHashSet();
				potentialRightSides.put(key, rightValues);
			}
			leftValues.put(value);
			rightValues.add(value);
		}
		
		interestingLeftSides = null;
		
		int size = potentialRightSides.size();
		int nextSize = 0;
		
		// Compute
		do{
			ShareableHashMap<IValue, ShareableValuesHashSet> rightSides = potentialRightSides;
			potentialRightSides = new ShareableHashMap<IValue, ShareableValuesHashSet>();
			
			for(int i = 0; i < size; i++){
				IValue leftKey = iLeftKeys.get();
				RotatingQueue<IValue> leftValues = iLefts.get();
				
				RotatingQueue<IValue> interestingLeftValues = null;
				
				IValue rightKey;
				while((rightKey = leftValues.get()) != null){
					ShareableValuesHashSet rightValues = rightSides.get(rightKey);
					if(rightValues != null){
						Iterator<IValue> rightValuesIterator = rightValues.iterator();
						while(rightValuesIterator.hasNext()){
							IValue rightValue = rightValuesIterator.next();
							if(allData.add(new Tuple(tupleType, new IValue[]{leftKey, rightValue}))){
								if(interestingLeftValues == null){
									nextSize++;
									
									iLeftKeys.put(leftKey);
									interestingLeftValues = new RotatingQueue<IValue>();
									iLefts.put(interestingLeftValues);
								}
								interestingLeftValues.put(rightValue);
								
								ShareableValuesHashSet potentialRightValues = potentialRightSides.get(rightKey);
								if(potentialRightValues == null){
									potentialRightValues = new ShareableValuesHashSet();
									potentialRightSides.put(rightKey, potentialRightValues);
								}
								potentialRightValues.add(rightValue);
							}
						}
					}
				}
			}
			size = nextSize;
			nextSize = 0;
		}while(size > 0);
		
		return allData;
	}
	
	public IRelation closure(){
		if(elementType == voidType) return this;
		if(!isReflexive()) throw new IllegalOperationException("closure", setType);
		
		Type tupleElementType = elementType.getFieldType(0).lub(elementType.getFieldType(1));
		Type tupleType = typeFactory.tupleType(tupleElementType, tupleElementType);
		
		return new RelationWriter(elementType, computeClosure(tupleType)).done();
	}
	
	public IRelation closureStar(){
		if(elementType == voidType) return this;
		if(!isReflexive()) throw new IllegalOperationException("closureStar", setType);
		
		Type tupleElementType = elementType.getFieldType(0).lub(elementType.getFieldType(1));
		Type tupleType = typeFactory.tupleType(tupleElementType, tupleElementType);
		
		ShareableValuesHashSet closure = computeClosure(tupleType);
		ShareableValuesHashSet carrier = computeCarrier();
		
		Iterator<IValue> carrierIterator = carrier.iterator();
		while(carrierIterator.hasNext()){
			IValue element = carrierIterator.next();
			closure.add(new Tuple(tupleType, new IValue[]{element, element}));
		}
		
		return new RelationWriter(elementType, closure).done();
	}
	
	public ISet select(int... indexes){
		ShareableValuesHashSet newData = new ShareableValuesHashSet();
		
		Iterator<IValue> dataIterator = data.iterator();
		while(dataIterator.hasNext()){
			ITuple tuple = (ITuple) dataIterator.next();
			
			newData.add(tuple.select(indexes));
		}
		
		Type type = getFieldTypes().select(indexes);
		return createSetWriter(type, newData).done();
	}
	
	public ISet select(String... fields){
		if(!elementType.hasFieldNames()) throw new IllegalOperationException("select with field names", setType);
		
		ShareableValuesHashSet newData = new ShareableValuesHashSet();
		
		Iterator<IValue> dataIterator = data.iterator();
		while(dataIterator.hasNext()){
			ITuple tuple = (ITuple) dataIterator.next();
			
			newData.add(tuple.select(fields));
		}
		
		Type type = getFieldTypes().select(fields);
		return createSetWriter(type, newData).done();
	}
	
	public int hashCode(){
		return data.hashCode();
	}
	
	public boolean equals(Object o){
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			Relation otherRelation = (Relation) o;
			
			if((setType != otherRelation.setType)) return false;
			
			return data.equals(otherRelation.data);
		}
		
		return false;
	}
	
	private Type determainMostGenericTypeInTuple(){
		Type result = elementType.getFieldType(0);
		for(int i = elementType.getArity() - 1; i > 0; i--){
			result = result.lub(elementType.getFieldType(i));
		}
		
		return result;
	}
	
	private boolean isReflexive(){
		if(elementType.getArity() != 2) throw new RuntimeException("Tuple is not binary");
		
		Type left = elementType.getFieldType(0);
		Type right = elementType.getFieldType(1);
			
		return right.comparable(left);
	}
}
