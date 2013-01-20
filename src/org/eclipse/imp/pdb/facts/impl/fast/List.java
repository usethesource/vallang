/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Paul Klint - added new methods
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListRelation;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.fast.ListWriter;
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
/*package*/ class List extends Value implements IList{
	protected final static TypeFactory typeFactory = TypeFactory.getInstance();
	
	protected final Type listType;
	protected final Type elementType;
	
	protected final ShareableValuesList data;
	protected int hashCode = 0;

	/*package*/ List(Type elementType, ShareableValuesList data){
		super();

		this.listType = typeFactory.listType(elementType);
		this.elementType = elementType;
		
		this.data = data;
		this.hashCode = data.hashCode();
	}
	
	/*package*/ static ListWriter createListWriter(Type eltType){
		return new ListWriter(eltType);
	}

	/*package*/ static ListWriter createListWriter(){
		return new ListWriter();
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
	
	@SuppressWarnings("unchecked")
	public <ListOrRel extends IList> ListOrRel  append(IValue element){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.append(element);

		Type newElementType = elementType.lub(element.getType());
		return (ListOrRel) new ListWriter(newElementType, newData).done();
	}

	@SuppressWarnings("unchecked")
	public <ListOrRel extends IList> ListOrRel concat(IList other){
		ShareableValuesList newData = new ShareableValuesList(data);
		Iterator<IValue> otherIterator = other.iterator();
		while(otherIterator.hasNext()){
			newData.append(otherIterator.next());
		}
		
		Type newElementType = elementType.lub(other.getElementType());
		return (ListOrRel) new ListWriter(newElementType, newData).done();
	}

	@SuppressWarnings("unchecked")
	public <ListOrRel extends IList> ListOrRel insert(IValue element){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.insert(element);

		Type newElementType = elementType.lub(element.getType());
		return (ListOrRel) new ListWriter(newElementType, newData).done();
	}
	
	@SuppressWarnings("unchecked")
	public  <ListOrRel extends IList> ListOrRel put(int index, IValue element) throws IndexOutOfBoundsException{
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.set(index, element);

		Type newElementType = elementType.lub(element.getType());
		return (ListOrRel) new ListWriter(newElementType, newData).done();
	}
	
	@SuppressWarnings("unchecked")
	public <ListOrRel extends IList> ListOrRel replace(int first, int second, int end, IList repl)
			throws FactTypeUseException, IndexOutOfBoundsException {
		ShareableValuesList newData = new ShareableValuesList();
		int rlen = repl.length();
		int increment = Math.abs(second - first);
		if(first < end){
			int dataIndex = 0;
			// Before begin
			while(dataIndex < first){
				newData.append(data.get(dataIndex++));
			}
			int replIndex = 0;
			boolean wrapped = false;
			// Between begin and end
			while(dataIndex < end){
				newData.append(repl.get(replIndex++));
				if(replIndex == rlen){
					replIndex = 0;
					wrapped = true;
				}
				dataIndex++; //skip the replaced element
				for(int j = 1; j < increment && dataIndex < end; j++){
					newData.append(data.get(dataIndex++));
				}
			}
			if(!wrapped){
				while(replIndex < rlen){
					newData.append(repl.get(replIndex++));
				}
			}
			// After end
			int dlen = data.size();
			while( dataIndex < dlen){
				newData.append(data.get(dataIndex++));
			}
		} else {
			// Before begin (from right to left)
			int dataIndex = data.size() - 1;
			while(dataIndex > first){
				newData.insert(data.get(dataIndex--));
			}
			// Between begin (right) and end (left)
			int replIndex = 0;
			boolean wrapped = false;
			while(dataIndex > end){
				newData.insert(repl.get(replIndex++));
				if(replIndex == repl.length()){
					replIndex = 0;
					wrapped = true;
				}
				dataIndex--; //skip the replaced element
				for(int j = 1; j < increment && dataIndex > end; j++){
					newData.insert(data.get(dataIndex--));
				}
			}
			if(!wrapped){
				while(replIndex < rlen){
					newData.insert(repl.get(replIndex++));
				}
			}
			// Left of end
			while(dataIndex >= 0){
				newData.insert(data.get(dataIndex--));
			}
		}
		Type newElementType = elementType.lub(repl.getElementType());
		return (ListOrRel) new ListWriter(newElementType, newData).done();
	}
	
	@SuppressWarnings("unchecked")
	public  <ListOrRel extends IList> ListOrRel delete(int index){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.remove(index);
		
		return (ListOrRel) new ListWriter(elementType, newData).done();
	}
	
	@SuppressWarnings("unchecked")
	public  <ListOrRel extends IList> ListOrRel delete(IValue element){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.remove(element);
		
		return (ListOrRel) new ListWriter(elementType, newData).done();
	}

	@SuppressWarnings("unchecked")
	public  <ListOrRel extends IList> ListOrRel reverse(){
		ShareableValuesList newData = new ShareableValuesList(data);
		newData.reverse();
		
		return (ListOrRel) new ListWriter(elementType, newData).done();
	}
	
	@SuppressWarnings("unchecked")
	public  <ListOrRel extends IList> ListOrRel sublist(int offset, int length){
		ShareableValuesList newData = data.subList(offset, length);
		
		return (ListOrRel) new ListWriter(elementType, newData).done();
	}
	
	public int hashCode(){
		return hashCode;
	}

	public boolean equals(Object o){
		if(o == this) return true;
		if(o == null) return false;
		
		if(o instanceof List) {
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
	
	public IListRelation product(IList lst){
		Type resultType = TypeFactory.getInstance().tupleType(getElementType(),lst.getElementType());
		ListRelationWriter w = new ListRelationWriter(resultType);

		for(IValue t1 : this){
			for(IValue t2 : lst){
				IValue vals[] = {t1, t2};
				ITuple t3 = new Tuple(resultType, vals);
				w.insert(t3);
			}
		}

		return (IListRelation) w.done();
	}

	@SuppressWarnings("unchecked")
	public <ListOrRel extends IList> ListOrRel intersect(IList other) {
		IListWriter w = ValueFactory.getInstance().listWriter(other.getElementType().lub(getElementType()));
		List o = (List) other;
		
		for(IValue v : data){
			if(o.data.contains(v)){
				w.append(v);
			}
		}
		
		return (ListOrRel) w.done();
	}
	
	@SuppressWarnings("unchecked")
	public <ListOrRel extends IList> ListOrRel subtract(IList lst) {
		IListWriter w = ValueFactory.getInstance().listWriter(lst.getElementType().lub(getElementType()));
		for (IValue v: this.data) {
			if (lst.contains(v)) {
				lst = lst.delete(v);
			} else
				w.append(v);
		}
		return (ListOrRel) w.done();
	}

	public boolean isSubListOf(IList lst) {
		int j = 0;
		nextchar:
			for(IValue elm : this.data){
				while(j < lst.length()){
					if(elm.isEqual(lst.get(j))){
						j++;
						continue nextchar;
					} else
						j++;
				}
				return false;
			}
		return true;
	}
}
