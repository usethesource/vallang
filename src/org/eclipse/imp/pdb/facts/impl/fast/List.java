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
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of IList.
 * 
 * @author Arnold Lankamp
 */
public class List extends Value implements IList{
	protected final static TypeFactory typeFactory = TypeFactory.getInstance();
	
	protected final Type listType;
	protected final Type elementType;
	
	protected final ShareableValuesList data;
	protected int hashCode = 0;

	protected List(Type elementType, ShareableValuesList data){
		super();

		this.listType = typeFactory.listType(elementType);
		this.elementType = elementType;
		
		this.data = data;
		this.hashCode = data.hashCode();
	}

	public Type getType(){
		return listType;
	}

	public Type getElementType(){
		return elementType;
	}

	public int length(){
		return data.size();
	}

	public boolean isEmpty(){
		return length() == 0;
	}

	public IValue get(int index){
		return data.get(index);
	}
	
	public boolean contains(IValue element){
		return data.contains(element);
	}

	public Iterator<IValue> iterator(){
		return data.iterator();
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitList(this);
	}
	
	public IList append(IValue element){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.append(element);

		Type newElementType = elementType.lub(element.getType());
		return new ListWriter(newElementType, newData).done();
	}

	public IList concat(IList other){
		ShareableValuesList newData = new ShareableValuesList(data);
		Iterator<IValue> otherIterator = other.iterator();
		while(otherIterator.hasNext()){
			newData.append(otherIterator.next());
		}
		
		Type newElementType = elementType.lub(other.getElementType());
		return new ListWriter(newElementType, newData).done();
	}

	public IList insert(IValue element){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.insert(element);

		Type newElementType = elementType.lub(element.getType());
		return new ListWriter(newElementType, newData).done();
	}
	
	public IList put(int index, IValue element) throws IndexOutOfBoundsException{
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.set(index, element);

		Type newElementType = elementType.lub(element.getType());
		return new ListWriter(newElementType, newData).done();
	}
	
	public IList delete(int index){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.remove(index);
		
		return new ListWriter(elementType, newData).done();
	}
	
	public IList delete(IValue element){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.remove(element);
		
		return new ListWriter(elementType, newData).done();
	}

	public IList reverse(){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.reverse();
		
		return new ListWriter(elementType, newData).done();
	}
	
	public IList sublist(int offset, int length){
		ShareableValuesList newData = data.subList(offset, length);
		
		return new ListWriter(elementType, newData).done();
	}
	
	public int hashCode(){
		return hashCode;
	}

	public boolean equals(Object o){
		if(o == this) return true;
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			List otherList = (List) o;
			
			if (getType() != otherList.getType()) return false;
			
			if (hashCode != otherList.hashCode) return false;
			
			if (listType != otherList.listType) return false;
			
			return data.equals(otherList.data);
		}
		
		return false;
	}

	public boolean isEqual(IValue value){
		if(value == this) return true;
		if(value == null) return false;
		
		if(value instanceof List){
			List otherList = (List) value;
			
			return data.isEqual(otherList.data);
		}
		
		return false;
	}
}
