/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation & CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *    Jurgen Vinju (jurgen@vinju.org)
 ********************************************************************************/

package org.eclipse.imp.pdb.facts.impl.hash;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.impl.Writer;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

public class List extends Value implements IList {
	private final Type eltType;
	private final LinkedList<IValue> content;

	@SuppressWarnings("unchecked")
	private List(List other, String label, IValue anno){
		super(other, label, anno);
		
		eltType = other.eltType;
		content = (LinkedList<IValue>) other.content.clone();
	}
	
	public List(Type eltType, LinkedList<IValue> listContent,
			HashMap<String, IValue> annotations) {
		super(TypeFactory.getInstance().listType(eltType), annotations);
		
		this.eltType = eltType;
		this.content = listContent;
	}

	public Type getElementType(){
		return eltType;
	}

	public Iterator<IValue> iterator(){
		return content.iterator();
	}

	public int length(){
		return content.size();
	}

	public boolean isEmpty(){
		return content.isEmpty();
	}

	public IValue get(int i){
		return content.get(i);
	}
	
	public IList put(int i, IValue elem) throws IndexOutOfBoundsException {
		ListWriter w = new ListWriter(elem.getType().lub(getElementType()));
		w.appendAll(this);
		w.replaceAt(i, elem);
		return w.done();
	}

	public IList insert(IValue elem) {
		ListWriter w = new ListWriter(elem.getType().lub(getElementType()));
		w.appendAll(this);
		w.insert(elem);
		w.setAnnotations(fAnnotations);
		
		return w.done();
	}

	public IList append(IValue elem) {
		ListWriter w = new ListWriter(elem.getType().lub(getElementType()));
		w.appendAll(this);
		w.append(elem);
		w.setAnnotations(fAnnotations);
		
		return w.done();
	}

	public IList reverse(){
		ListWriter w = new ListWriter(getElementType());
		for (IValue e : this) {
			w.insert(e);
		}
		w.setAnnotations(fAnnotations);
		return w.done();
	}
	
	public IList concat(IList other) {
		ListWriter w = new ListWriter(getElementType().lub(other.getElementType()));
		w.appendAll(this);
		w.appendAll(other);
		return w.done();
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitList(this);
	}
	
	protected IValue clone(String label, IValue anno) {
		return new List(this, label, anno);
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("[");
		
		Iterator<IValue> listIterator = iterator();
		if(listIterator.hasNext()){
			sb.append(listIterator.next());
			
			while(listIterator.hasNext()){
				sb.append(",");
				sb.append(listIterator.next());
			}
		}
		
		sb.append("]");
		
		return sb.toString();
	}
	
	public boolean equals(Object o){
		if(!(o instanceof List)){
			return false;
		}
		List other = (List) o;
		return content.equals(other.content);
	}
	
	@Override
	public int hashCode() {
		return content.hashCode();
	}
	
    /*package*/ static ListWriter createListWriter(Type eltType){
		return new ListWriter(eltType);
	}
	
	private static void checkInsert(IValue elem, Type eltType) throws FactTypeError{
		Type type = elem.getType();
		if(!type.isSubtypeOf(eltType)){
			throw new FactTypeError("Element type " + type + " is not compatible with " + eltType);
		}
	}
	
	/**
	 * This class does not guarantee thread-safety. Users must lock the writer object for thread safety.
	 * It is thread-friendly however.
	 */
	private static class ListWriter extends Writer implements IListWriter{
		private final Type eltType;
		private final LinkedList<IValue> listContent;
		
		private List constructedList;
		
		public ListWriter(Type eltType){
			super();
			
			this.eltType = eltType;
			listContent = new LinkedList<IValue>();
			
			constructedList = null;
		}
		
		private void checkMutation(){
			if(constructedList != null) throw new UnsupportedOperationException("Mutation of a finalized list is not supported.");
		}
		
		private void put(int index, IValue elem){
			checkInsert(elem, eltType);
			listContent.add(index, elem);
		}
		
		public void insert(IValue elem) throws FactTypeError{
			checkMutation();
			
			put(0, elem);
		}
		
		public void insert(IValue[] elems, int start, int length) throws FactTypeError{
			checkMutation();
			checkBounds(elems, start, length);
			
			for(int i = start + length - 1; i >= start; i--){
				put(0, elems[i]);
			}
		}

		public void replaceAt(int index, IValue elem) throws FactTypeError, IndexOutOfBoundsException {
			checkMutation();
			checkInsert(elem, eltType);
			listContent.set(index, elem);
		}
		
		public void insert(IValue... elems) throws FactTypeError{
			insert(elems, 0, elems.length);
		}
		
		public void insert(int index, IValue elem) throws FactTypeError{
			checkMutation();
			
			put(index, elem);
		}
		
		public void insertAt(int index, IValue[] elems, int start, int length) throws FactTypeError{
			checkMutation();
			checkBounds(elems, start, length);
			
			for(int i = start + length - 1; i >= start; i--){
				put(index, elems[i]);
			}
		}

		public void insertAt(int index, IValue... elems) throws FactTypeError{
			insertAt(index,  elems, 0, 0);
		}
		
		public void append(IValue elem) throws FactTypeError{
			checkMutation();
			
			put(listContent.size(), elem);
		}

		public void append(IValue... elems) throws FactTypeError{
			checkMutation();
			
			for(IValue elem : elems){
				put(listContent.size(), elem);
			}
		}
		
		public void appendAll(Iterable<? extends IValue> collection) throws FactTypeError{
			checkMutation();
			
			for(IValue v : collection){
				put(listContent.size(), v);
			}
		}

		public IList done(){
			if(constructedList == null) constructedList = new List(eltType, listContent, fAnnotations);
			
			return constructedList;
		}
		
		private void checkBounds(IValue[] elems, int start, int length){
			if(start < 0) throw new ArrayIndexOutOfBoundsException("start < 0");
			if((start + length) > elems.length) throw new ArrayIndexOutOfBoundsException("(start + length) > elems.length");
		}
	}
}