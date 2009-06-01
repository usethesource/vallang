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

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.List;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * Implementation of shareable lists.
 * 
 * @author Arnold Lankamp
 */
public class SharedList extends List implements IShareable{
	
	protected SharedList(Type elementType, ShareableValuesList data){
		super(elementType, data);
	}
	
	public IList append(IValue element){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.append(element);
		
		Type newElementType = elementType.lub(element.getType());
		return SharedValueFactory.getInstance().createListWriter(newElementType, newData).done();
	}

	public IList concat(IList other){
		ShareableValuesList newData = new ShareableValuesList(data);
		Iterator<IValue> otherIterator = other.iterator();
		while(otherIterator.hasNext()){
			newData.append(otherIterator.next());
		}
		
		Type newElementType = elementType.lub(other.getElementType());
		return SharedValueFactory.getInstance().createListWriter(newElementType, newData).done();
	}

	public IList insert(IValue element){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.insert(element);

		Type newElementType = elementType.lub(element.getType());
		return SharedValueFactory.getInstance().createListWriter(newElementType, newData).done();
	}
	
	public IList put(int index, IValue element) throws IndexOutOfBoundsException{
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.set(index, element);
		
		Type newElementType = elementType.lub(element.getType());
		return SharedValueFactory.getInstance().createListWriter(newElementType, newData).done();
	}
	
	public IList delete(int index){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.remove(index);
		
		return SharedValueFactory.getInstance().createListWriter(elementType, newData).done();
	}
	
	public IList delete(IValue element){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.remove(element);
		
		return SharedValueFactory.getInstance().createListWriter(elementType, newData).done();
	}

	public IList reverse(){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.reverse();
		
		return SharedValueFactory.getInstance().createListWriter(elementType, newData).done();
	}
	
	public IList sublist(int offset, int length){
		ShareableValuesList newData = data.subList(offset, length);
		
		return SharedValueFactory.getInstance().createListWriter(elementType, newData).done();
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
