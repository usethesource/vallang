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
import java.util.NoSuchElementException;

import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of ITuple.
 * 
 * @author Arnold Lankamp
 */
public class Tuple implements ITuple{
	protected final static TypeFactory typeFactory = TypeFactory.getInstance();
	
	protected final Type tupleType;
	protected final IValue[] elements;

	protected Tuple(IValue[] elements){
		super();
		
		this.tupleType = typeFactory.tupleType(elements);
		
		this.elements = elements;
	}

	public Type getType(){
		return tupleType;
	}

	public int arity(){
		return elements.length;
	}

	public IValue get(int i){
		return elements[i];
	}

	public IValue get(String label){
		return elements[tupleType.getFieldIndex(label)];
	}
	
	public Iterator<IValue> iterator(){
		return new TupleIterator(this);
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitTuple(this);
	}

	public ITuple set(int i, IValue arg){
		int nrOfElements = elements.length;
		IValue[] newElements = new IValue[nrOfElements];
		System.arraycopy(elements, 0, newElements, 0, nrOfElements);
		
		newElements[i] = arg;
		
		return ValueFactory.getInstance().createTupleUnsafe(newElements);
	}

	public ITuple set(String label, IValue arg){
		int nrOfElements = elements.length;
		IValue[] newElements = new IValue[nrOfElements];
		System.arraycopy(elements, 0, newElements, 0, nrOfElements);
		
		newElements[tupleType.getFieldIndex(label)] = arg;
		
		return ValueFactory.getInstance().createTupleUnsafe(newElements);
	}

	public IValue select(int... indexes){
		if(indexes.length == 1) return get(indexes[0]);
		
		IValue[] elements = new IValue[indexes.length];
		for(int i = 0; i < indexes.length; i++){
			elements[i] = get(indexes[i]);
		}
		
		return ValueFactory.getInstance().createTupleUnsafe(elements);
	}

	public IValue select(String... fields){
		if(fields.length == 1) return get(fields[0]);
		
		IValue[] elements = new IValue[fields.length];
		for(int i = fields.length - 1; i >= 0; i--){
			elements[i] = get(fields[i]);
		}
		
		return ValueFactory.getInstance().createTupleUnsafe(elements);
	}
	
	public int hashCode(){
		int hash = tupleType.hashCode();
		
		for(int i = elements.length - 1; i >= 0; i--){
			hash = (hash << 23) + (hash >> 5);
			hash ^= elements[i].hashCode();
		}
		
		return hash;
	}

	public boolean equals(Object o){
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			Tuple otherTuple = (Tuple) o;
			
			if(tupleType != otherTuple.tupleType) return false;
			
			IValue[] otherElements = otherTuple.elements;
			int nrOfElements = elements.length;
			if(otherElements.length == nrOfElements){
				for(int i = nrOfElements - 1; i >= 0; i--){
					if(!otherElements[i].equals(elements[i])) return false;
				}
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isEqual(IValue value){
		if(value == this) return true;
		if(value == null) return false;
		
		if(value instanceof Tuple){
			Tuple otherTuple = (Tuple) value;
			
			if(!tupleType.comparable(otherTuple.tupleType)) return false;
			
			IValue[] otherElements = otherTuple.elements;
			int nrOfElements = elements.length;
			if(otherElements.length == nrOfElements){
				for(int i = nrOfElements - 1; i >= 0; i--){
					if(!otherElements[i].isEqual(elements[i])) return false;
				}
				return true;
			}
		}
		
		return false;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("<");
		
		int size = elements.length;
		if(size > 0){
			int i = 0;
			sb.append(elements[i]);
			
			for(i = 1; i < size; i++){
				sb.append(",");
				sb.append(elements[i]);
			}
		}
		
		sb.append(">");
		
		return sb.toString();
	}
	
	private static class TupleIterator implements Iterator<IValue>{
		private final IValue[] elements;
		private int index = 0;
		
		public TupleIterator(Tuple tuple){
			super();
			
			elements = tuple.elements;
		}
		
		public boolean hasNext(){
			return index < elements.length;
		}
		
		public IValue next(){
			if(!hasNext()) throw new NoSuchElementException("No more elements in this iteration.");
			
			return elements[index++];
		}
		
		public void remove(){
			throw new UnsupportedOperationException("This iterator doesn't support removal.");
		}
	}
}
