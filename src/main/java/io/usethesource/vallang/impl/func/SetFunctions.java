/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse public static License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *
 * Based on code by:
 *
 *   * Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/
package io.usethesource.vallang.impl.func;

import java.util.Iterator;
import java.util.function.Function;

import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.impl.util.collections.ShareableValuesHashSet;
import io.usethesource.vallang.impl.util.collections.ShareableValuesList;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.util.RotatingQueue;
import io.usethesource.vallang.util.ShareableHashMap;
import io.usethesource.vallang.util.ValueIndexedHashMap;

public final class SetFunctions {

	private final static TypeFactory TF = TypeFactory.getInstance();

	public static ISet compose(IValueFactory vf, ISet set1, ISet set2) throws FactTypeUseException {
		if (set1.getElementType().isBottom())
			return set1;
		if (set2.getElementType().isBottom())
			return set2;

		if (set1.getElementType().getArity() != 2
				|| set2.getElementType().getArity() != 2) {
			throw new IllegalOperationException(
					"Incompatible types for composition.",
					set1.getElementType(), set2.getElementType());
		}
		
		if (!set1.getElementType().getFieldType(1)
				.comparable(set2.getElementType().getFieldType(0)))
			return vf.set();
		
		// Index
		ShareableHashMap<IValue, ShareableValuesList> rightSides = new ShareableHashMap<>();
		
		Iterator<IValue> otherRelationIterator = set2.iterator();
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
		ISetWriter resultWriter = vf.setWriter();
		
		Iterator<IValue> relationIterator = set1.iterator();
		while(relationIterator.hasNext()){
			ITuple thisTuple = (ITuple) relationIterator.next();
			
			IValue key = thisTuple.get(1);
			ShareableValuesList values = rightSides.get(key);
			if(values != null){
				Iterator<IValue> valuesIterator = values.iterator();
				do{
					IValue value = valuesIterator.next();
					IValue[] newTupleData = new IValue[]{thisTuple.get(0), value};
					resultWriter.insert(vf.tuple(newTupleData));
				}while(valuesIterator.hasNext());
			}
		}
		
		return resultWriter.done();
	}	

	public static ISet carrier(IValueFactory vf, ISet set1) {
		ISetWriter w = vf.setWriter();

		for (IValue t : set1) {
			w.insertAll((ITuple) t);
		}

		return w.done();
	}

	private static ShareableValuesHashSet computeClosureDelta(IValueFactory vf, ISet rel1, Type tupleType) {
		RotatingQueue<IValue> iLeftKeys = new RotatingQueue<>();
		RotatingQueue<RotatingQueue<IValue>> iLefts = new RotatingQueue<>();
		
		ValueIndexedHashMap<RotatingQueue<IValue>> interestingLeftSides = new ValueIndexedHashMap<>();
		ValueIndexedHashMap<ShareableValuesHashSet> potentialRightSides = new ValueIndexedHashMap<>();
		
		// Index
		Iterator<IValue> allDataIterator = rel1.iterator();
		while(allDataIterator.hasNext()){
			ITuple tuple = (ITuple) allDataIterator.next();

			IValue key = tuple.get(0);
			IValue value = tuple.get(1);
			RotatingQueue<IValue> leftValues = interestingLeftSides.get(key);
			ShareableValuesHashSet rightValues;
			if(leftValues != null){
				rightValues = potentialRightSides.get(key);
			}else{
				leftValues = new RotatingQueue<>();
				iLeftKeys.put(key);
				iLefts.put(leftValues);
				interestingLeftSides.put(key, leftValues);
				
				rightValues = new ShareableValuesHashSet();
				potentialRightSides.put(key, rightValues);
			}
			leftValues.put(value);
			rightValues.add(value);
		}
		
		int size = potentialRightSides.size();
		int nextSize = 0;
		
		// Compute
		final ShareableValuesHashSet newTuples = new ShareableValuesHashSet();
		do{
			ValueIndexedHashMap<ShareableValuesHashSet> rightSides = potentialRightSides;
			potentialRightSides = new ValueIndexedHashMap<>();
			
			for(; size > 0; size--){
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
							if(newTuples.add(vf.tuple(leftKey, rightValue))){
								if(interestingLeftValues == null){
									nextSize++;
									
									iLeftKeys.put(leftKey);
									interestingLeftValues = new RotatingQueue<>();
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
		
		return newTuples;
	}
	
	public static ISet closure(IValueFactory vf, ISet rel1) {
		if (rel1.getElementType().isBottom())
			return rel1;
		if (!isBinary(rel1))
			throw new IllegalOperationException("closure", rel1.getType());

		Type tupleElementType = rel1.getElementType().getFieldType(0).lub(rel1.getElementType().getFieldType(1));
		Type tupleType = TF.tupleType(tupleElementType, tupleElementType);

		java.util.Set<IValue> closureDelta = computeClosureDelta(vf, rel1, tupleType);

		// NOTE: type is already known, thus, using a SetWriter degrades performance
		ISetWriter resultWriter = vf.setWriter();
		resultWriter.insertAll(rel1);
		resultWriter.insertAll(closureDelta);

		return resultWriter.done();
	}

	// TODO: Currently untested in PDB.
	public static ISet closureStar(IValueFactory vf, ISet rel1) {
		if (rel1.getElementType().isBottom())
			return rel1;
		if (!isBinary(rel1))
			throw new IllegalOperationException("closureStar", rel1.getType());

		Type tupleElementType = rel1.getElementType().getFieldType(0).lub(rel1.getElementType().getFieldType(1));
		Type tupleType = TF.tupleType(tupleElementType, tupleElementType);

		// calculate
		ShareableValuesHashSet closureDelta = computeClosureDelta(vf, rel1, tupleType);
		ISet carrier = carrier(vf, rel1);

		// aggregate result
		// NOTE: type is already known, thus, using a SetWriter degrades performance
		ISetWriter resultWriter = vf.setWriter();
		resultWriter.insertAll(rel1);
		resultWriter.insertAll(closureDelta);
		
		Iterator<IValue> carrierIterator = carrier.iterator();
		while (carrierIterator.hasNext()) {
			IValue element = carrierIterator.next();
			resultWriter.insert(vf.tuple(element, element));
		}

		return resultWriter.done();
	}
	
	private static boolean isBinary(ISet rel1){
		return rel1.getElementType().getArity() == 2;
	}
	
	public static ISet domain(IValueFactory vf, ISet set1) {
		int columnIndex = 0;
		ISetWriter w = vf.setWriter();

		for (IValue elem : set1) {
			ITuple tuple = (ITuple) elem;
			w.insert(tuple.get(columnIndex));
		}
		return w.done();
	}

	public static ISet range(IValueFactory vf, ISet set1) {
		int columnIndex = set1.getType().getArity() - 1;
		ISetWriter w = vf.setWriter();

		for (IValue elem : set1) {
			ITuple tuple = (ITuple) elem;
			w.insert(tuple.get(columnIndex));
		}

		return w.done();
	}

	public static ISet project(IValueFactory vf, ISet set1, int... fields) {
		ISetWriter w = vf.setWriter();

		for (IValue v : set1) {
			w.insert(((ITuple) v).select(fields));
		}

		return w.done();
	}

	public static ISet projectByFieldNames(IValueFactory vf, ISet set1,
			String... fields) {
		int[] indexes = new int[fields.length];
		int i = 0;

		if (set1.getType().getFieldTypes().hasFieldNames()) {
			for (String field : fields) {
				indexes[i++] = set1.getType().getFieldTypes()
						.getFieldIndex(field);
			}

			return project(vf, set1, indexes);
		}

		throw new IllegalOperationException("select with field names",
				set1.getType());
	}

    public static ISet index(IValueFactory vf, ISet set1, IValue key) {
        Type elementType = set1.getElementType();
        
        if (elementType.isBottom()) {
            return vf.setWriter().done();
        }
        
        int valueArity = elementType.getArity() - 1;

        Function<ITuple, IValue> mapper;
        if (valueArity == 0) {
            mapper = t -> t.get(1);
        }
        else {
            int[] newTupleIndex = new int[valueArity];
            for (int k = 1; k <= valueArity; k++) {
                newTupleIndex[k - 1] =  k;
            }
            mapper = t -> t.select(newTupleIndex);
        }
        
        ISetWriter result = vf.setWriter();
        for (IValue r : set1) {
            ITuple tup = (ITuple)r;
            if (tup.get(0).isEqual(key)) {
                result.insert(mapper.apply(tup));
            }
        }
        return result.done();
    }

}
