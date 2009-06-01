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

/**
 * Implementation of shareable tuples.
 * 
 * @author Arnold Lankamp
 */
public class SharedTuple extends Tuple implements IShareable{
	
	protected SharedTuple(IValue[] elements){
		super(elements);
	}

	public ITuple set(int i, IValue arg){
		int nrOfElements = elements.length;
		IValue[] newElements = new IValue[nrOfElements];
		System.arraycopy(elements, 0, newElements, 0, nrOfElements);
		
		newElements[i] = arg;
		
		return SharedValueFactory.getInstance().createTupleUnsafe(newElements);
	}

	public ITuple set(String label, IValue arg){
		int nrOfElements = elements.length;
		IValue[] newElements = new IValue[nrOfElements];
		System.arraycopy(elements, 0, newElements, 0, nrOfElements);
		
		newElements[tupleType.getFieldIndex(label)] = arg;
		
		return SharedValueFactory.getInstance().createTupleUnsafe(newElements);
	}

	public IValue select(int... indexes){
		if(indexes.length == 1) return get(indexes[0]);
		
		IValue[] elements = new IValue[indexes.length];
		for(int i = 0; i < indexes.length; i++){
			elements[i] = get(indexes[i]);
		}
		
		return SharedValueFactory.getInstance().createTupleUnsafe(elements);
	}

	public IValue select(String... fields){
		if(fields.length == 1) return get(fields[0]);
		
		IValue[] elements = new IValue[fields.length];
		for(int i = fields.length - 1; i >= 0; i--){
			elements[i] = get(fields[i]);
		}
		
		return SharedValueFactory.getInstance().createTupleUnsafe(elements);
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
