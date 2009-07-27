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

import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.Tuple;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * Implementation of shareable tuples.
 * 
 * @author Arnold Lankamp
 */
public class SharedTuple extends Tuple implements IShareable{
	
	protected SharedTuple(Type tupleType, IValue[] elements){
		super(tupleType, elements);
	}

	public ITuple set(int index, IValue arg){
		int nrOfElements = elements.length;
		IValue[] newElements = new IValue[nrOfElements];
		Type[] elementTypes = new Type[nrOfElements];
		for(int i = nrOfElements - 1; i >= 0; i--){
			IValue element = elements[i];
			newElements[i] = element;
			elementTypes[i] = element.getType();
		}
		
		newElements[index] = arg;
		elementTypes[index] = arg.getType();
		
		return SharedValueFactory.getInstance().createTupleUnsafe(typeFactory.tupleType(elementTypes), newElements);
	}

	public ITuple set(String label, IValue arg){
		int nrOfElements = elements.length;
		IValue[] newElements = new IValue[nrOfElements];
		Type[] elementTypes = new Type[nrOfElements];
		for(int i = nrOfElements - 1; i >= 0; i--){
			IValue element = elements[i];
			newElements[i] = element;
			elementTypes[i] = element.getType();
		}
		
		newElements[tupleType.getFieldIndex(label)] = arg;
		elementTypes[tupleType.getFieldIndex(label)] = arg.getType();
		
		return SharedValueFactory.getInstance().createTupleUnsafe(typeFactory.tupleType(elementTypes), newElements);
	}

	public IValue select(int... indexes){
		if(indexes.length == 1) return get(indexes[0]);
		
		int nrOfElements = indexes.length;
		IValue[] elements = new IValue[nrOfElements];
		Type[] elementTypes = new Type[nrOfElements];
		for(int i = nrOfElements - 1; i >= 0 ; i--){
			IValue element = get(indexes[i]);
			elements[i] = element;
			elementTypes[i] = element.getType();
		}
		
		return SharedValueFactory.getInstance().createTupleUnsafe(typeFactory.tupleType(elementTypes), elements);
	}

	public IValue select(String... fields){
		if(fields.length == 1) return get(fields[0]);

		int nrOfElements = fields.length;
		IValue[] elements = new IValue[nrOfElements];
		Type[] elementTypes = new Type[nrOfElements];
		for(int i = nrOfElements - 1; i >= 0; i--){
			IValue element = get(fields[i]);
			elements[i] = element;
			elementTypes[i] = element.getType();
		}
		
		return SharedValueFactory.getInstance().createTupleUnsafe(typeFactory.tupleType(elementTypes),elements);
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
