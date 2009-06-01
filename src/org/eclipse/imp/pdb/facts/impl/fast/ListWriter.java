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

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
import org.eclipse.imp.pdb.facts.type.Type;

// TODO Add checking.
/**
 * Implementation of IListWriter.
 * 
 * @author Arnold Lankamp
 */
public class ListWriter implements IListWriter{
	protected final Type elementType;
	
	protected final ShareableValuesList data;
	
	protected IList constructedList;
	
	protected ListWriter(Type elementType){
		super();
		
		this.elementType = elementType;
		
		data = new ShareableValuesList();
		
		constructedList = null;
	}
	
	protected ListWriter(Type elementType, ShareableValuesList data){
		super();
		
		this.elementType = elementType;
		this.data = data;
		
		constructedList = null;
	}
	
	public void append(IValue element){
		checkMutation();
		
		data.append(element);
	}
	
	public void append(IValue... elems){
		checkMutation();
		
		for(IValue elem : elems){
			data.append(elem);
		}
	}
	
	public void appendAll(Iterable<? extends IValue> collection){
		checkMutation();
		
		Iterator<? extends IValue> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			data.append(collectionIterator.next());
		}
	}
	
	public void insert(IValue elem){
		checkMutation();
		
		data.insert(elem);
	}
	
	public void insert(IValue... elements){
		insert(elements, 0, elements.length);
	}
	
	public void insert(IValue[] elements, int start, int length){
		checkMutation();
		checkBounds(elements, start, length);
		
		for(int i = start + length - 1; i >= start; i--){
			data.insert(elements[i]);
		}
	}
	
	public void insertAll(Iterable<IValue> collection){
		checkMutation();
		
		Iterator<? extends IValue> collectionIterator = collection.iterator();
		while(collectionIterator.hasNext()){
			data.insert(collectionIterator.next());
		}
	}
	
	public void insertAt(int index, IValue element){
		checkMutation();
		
		data.insertAt(index, element);
	}
	
	public void insertAt(int index, IValue... elements){
		insertAt(index, elements, 0, 0);
	}
	
	public void insertAt(int index, IValue[] elements, int start, int length){
		checkMutation();
		checkBounds(elements, start, length);
		
		for(int i = start + length - 1; i >= start; i--){
			data.insertAt(index, elements[i]);
		}
	}
	
	public void replaceAt(int index, IValue element){
		checkMutation();
		
		data.set(index, element);
	}
	
	public void delete(int index){
		checkMutation();
		
		data.remove(index);
	}
	
	public void delete(IValue element){
		checkMutation();
		
		data.remove(element);
	}
	
	protected void checkMutation(){
		if(constructedList != null) throw new UnsupportedOperationException("Mutation of a finalized list is not supported.");
	}
	
	private void checkBounds(IValue[] elems, int start, int length){
		if(start < 0) throw new ArrayIndexOutOfBoundsException("start < 0");
		if((start + length) > elems.length) throw new ArrayIndexOutOfBoundsException("(start + length) > elems.length");
	}
	
	public IList done(){
		if(constructedList == null) constructedList = new List(elementType, data);
		
		return constructedList;
	}
}