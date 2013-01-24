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
 *    Paul Klint (Paul.Klint@cwi.nl)
 *    Michael Steindorfer (Michael.Steindorfer@cwi.nl)
 ********************************************************************************/

package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListRelation;
import org.eclipse.imp.pdb.facts.IListRelationWriter;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.impl.Writer;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

public class List extends Value implements IList {
	private final Type eltType;
	protected final java.util.List<IValue> content;
	private int fHash = 0;

	public List(Type eltType, java.util.List<IValue> listContent) {
		super(TypeFactory.getInstance().listType(eltType));
		
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
	
	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel sublist(int offset, int length) {
		if (offset < 0 || length < 0 || offset + length > content.size()) {
			throw new IndexOutOfBoundsException();
		}
		ListWriter w = new ListWriter(getElementType());
		for (int i = offset; i < offset + length; i++) {
			w.append(content.get(i));
		}
		return (IListOrRel) w.done();
	}
	
	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel put(int i, IValue elem) throws IndexOutOfBoundsException {
		ListWriter w = new ListWriter(elem.getType().lub(getElementType()));
		w.appendAll(this);
		w.replaceAt(i, elem);
		return (IListOrRel) w.done();
	}
	
	@SuppressWarnings("unchecked")
	public <ListOrRel extends IList> ListOrRel replace(int first, int second, int end, IList repl)
			throws FactTypeUseException, IndexOutOfBoundsException {
		ListWriter w = new ListWriter(repl.getElementType().lub(getElementType()));
		if(first < end){
			for(int i = 0; i < first ; i++){
				w.append(content.get(i));
			}
			w.appendAll(repl);
			for(int i = end; i < content.size() ; i++){
				w.append(content.get(i));
			}
		} else {
			for(int i = content.size() -1; i > first; i--){
				w.insert(content.get(i));
			}
			for(IValue v : repl){
				w.insert(v);
			}
			for(int i = end; i >= 0; i--){
				w.insert(content.get(i));
			}
		}
		return (ListOrRel) w.done();
	}

	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel insert(IValue elem) {
		ListWriter w = new ListWriter(elem.getType().lub(getElementType()));
		w.appendAll(this);
		w.insert(elem);
		
		return (IListOrRel) w.done();
	}

	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel append(IValue elem) {
		ListWriter w = new ListWriter(elem.getType().lub(getElementType()));
		w.appendAll(this);
		w.append(elem);
		
		return (IListOrRel) w.done();
	}

	public boolean contains(IValue e) {
		return content.contains(e);
	}

	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel delete(IValue e) {
		ListWriter w = new ListWriter(getElementType());
		w.appendAll(this);
		w.delete(e);
		return (IListOrRel) w.done();
	}
	
	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel delete(int i) {
		ListWriter w = new ListWriter(getElementType());
		w.appendAll(this);
		w.delete(i);
		return (IListOrRel) w.done();
	}

	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel reverse(){
		ListWriter w = new ListWriter(getElementType());
		for (IValue e : this) {
			w.insert(e);
		}
		return (IListOrRel) w.done();
	}
	
	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel concat(IList other) {
		ListWriter w = new ListWriter(getElementType().lub(other.getElementType()));
		w.appendAll(this);
		w.appendAll(other);
		return (IListOrRel) w.done();
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitList(this);
	}
	
	public boolean equals(Object o){
		if (o == this) {
			return true;
		}
		else if (o == null) {
			return false;
		}
		else if (o instanceof List) {
			List other = (List) o;
			
			return content.equals(other.content);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		if (fHash == 0) {
			fHash = content.hashCode();
		}
		return fHash;
	}
	
    /*package*/ static ListWriter createListWriter(Type eltType){
		return new ListWriter(eltType);
	}
    
    /*package*/ static ListWriter createListWriter(){
		return new ListWriter();
	}
	
	private static void checkInsert(IValue elem, Type eltType) throws FactTypeUseException{
		Type type = elem.getType();
		if(!type.isSubtypeOf(eltType)){
			throw new UnexpectedElementTypeException(eltType, type);
		}
	}
	
	/**
	 * This class does not guarantee thread-safety. Users must lock the writer object for thread safety.
	 * It is thread-friendly however.
	 */
	protected static class ListWriter extends Writer implements IListWriter{
		protected Type eltType;
		protected final java.util.List<IValue> listContent;
		
		protected IList constructedList;
		private final boolean inferred;
		
		public ListWriter(Type eltType){
			super();
			
			this.eltType = eltType;
			this.inferred = false;
			listContent = new LinkedList<IValue>();
			
			constructedList = null;
		}
		
		public ListWriter(){
			super();
			
			this.eltType = TypeFactory.getInstance().voidType();
			inferred = true;
			listContent = new LinkedList<IValue>();
			
			constructedList = null;
		}
		
		private void checkMutation(){
			if(constructedList != null) throw new UnsupportedOperationException("Mutation of a finalized list is not supported.");
		}
		
		private void put(int index, IValue elem){
			if (inferred) {
				eltType = eltType.lub(elem.getType());
			}
			else {
				checkInsert(elem, eltType);
			}
			listContent.add(index, elem);
		}
		
		public void insert(IValue elem) throws FactTypeUseException{
			checkMutation();
			put(0, elem);
		}
		
		public void insert(IValue[] elems, int start, int length) throws FactTypeUseException{
			checkMutation();
			checkBounds(elems, start, length);
			
			for(int i = start + length - 1; i >= start; i--){
				updateType(elems[i]);
				put(0, elems[i]);
			}
		}

		public void replaceAt(int index, IValue elem) throws FactTypeUseException, IndexOutOfBoundsException {
			checkMutation();
			updateType(elem);
			checkInsert(elem, eltType);
			listContent.set(index, elem);
		}
		
		public void insert(IValue... elems) throws FactTypeUseException{
			insert(elems, 0, elems.length);
		}
		
		public void insertAt(int index, IValue[] elems, int start, int length) throws FactTypeUseException{
			checkMutation();
			checkBounds(elems, start, length);
			
			for(int i = start + length - 1; i >= start; i--) {
				if (inferred) {
					eltType = eltType.lub(elems[i].getType());
				}
				put(index, elems[i]);
			}
		}

		public void insertAt(int index, IValue... elems) throws FactTypeUseException{
			insertAt(index,  elems, 0, 0);
		}
		
		public void append(IValue elem) throws FactTypeUseException{
			checkMutation();
			updateType(elem);
			put(listContent.size(), elem);
		}

		public void append(IValue... elems) throws FactTypeUseException{
			checkMutation();
			
			for(IValue elem : elems){
				updateType(elem);
				put(listContent.size(), elem);
			}
		}
		
		public void appendAll(Iterable<? extends IValue> collection) throws FactTypeUseException{
			checkMutation();
			
			for(IValue v : collection){
				updateType(v);
				put(listContent.size(), v);
			}
		}

		private void updateType(IValue v) {
			if (inferred) {
				eltType = eltType.lub(v.getType());
			}
		}
		
		public int size() {
			return listContent.size();
		}

		public IList done() {
			if (constructedList == null) {
				constructedList = ListOrRel.apply(eltType, listContent);
			}

			return constructedList;
		}
		
		private void checkBounds(IValue[] elems, int start, int length) {
			if(start < 0) throw new ArrayIndexOutOfBoundsException("start < 0");
			if((start + length) > elems.length) throw new ArrayIndexOutOfBoundsException("(start + length) > elems.length");
		}

		public void delete(IValue elem) {
			checkMutation();
			listContent.remove(elem);
		}
		
		public void delete(int i) {
			checkMutation();
			listContent.remove(i);
		}
		
	}
	
	public IListRelation product(IList lst){
		Type resultType = TypeFactory.getInstance().tupleType(getElementType(),lst.getElementType());
		IListRelationWriter w = new ListRelation.ListRelationWriter(resultType);

		for(IValue t1 : this){
			for(IValue t2 : lst){
				ITuple t3 = new Tuple(t1,t2);
				w.insert(t3);
			}
		}

		return w.done();
	}

	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel intersect(IList other) {
		IListWriter w = ValueFactory.getInstance().listWriter(other.getElementType().lub(getElementType()));
		List o = (List) other;
		
		for(IValue v : content){
			if(o.content.contains(v)){
				w.insert(v);
			}
		}
		
		return (IListOrRel) w.done();
	}

	@SuppressWarnings("unchecked")
	public <IListOrRel extends IList> IListOrRel subtract(IList lst) {
		IListWriter w = ValueFactory.getInstance().listWriter(lst.getElementType().lub(getElementType()));
		for (IValue v: this.content) {
			if (lst.contains(v)) {
				lst = lst.delete(v);
			} else
				w.append(v);
		}
		return (IListOrRel) w.done();
	}

	public boolean isSubListOf(IList lst) {
		int j = 0;
		nextchar:
			for(IValue elm : this.content){
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