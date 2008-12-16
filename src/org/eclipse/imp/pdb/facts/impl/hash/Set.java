/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.hash;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.impl.Writer;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.SetType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Set extends Value implements ISet{
	final HashSet<IValue> content;

	/*package*/ Set(SetType setType, HashSet<IValue> content){
		super(setType);
		
		this.content = content;
	}
	
	@SuppressWarnings("unchecked")
	protected Set(Set other, String label, IValue anno) {
		super(other, label, other);
		
		content = (HashSet<IValue>) other.content.clone();
	}

	/*package*/ Set(Type eltType, HashSet<IValue> setContent,
			HashMap<String, IValue> annotations) {
		super(TypeFactory.getInstance().setType(eltType), annotations);
		content = (HashSet<IValue>) setContent;
	}

	public boolean contains(IValue element) throws FactTypeError{
		return content.contains(element);
	}

	public boolean isEmpty(){
		return content.isEmpty();
	}

	public int size(){
		return content.size();
	}

	@SuppressWarnings("unchecked")
	public ISet insert(IValue element) {
		ISetWriter sw = ValueFactory.getInstance().setWriter(getElementType().lub(element.getType()));
		sw.insertAll(this);
		sw.insert(element);
		sw.setAnnotations(fAnnotations);
		return sw.done();
	}

	@SuppressWarnings("unchecked")
	public ISet intersect(ISet other) {
		ISetWriter w = ValueFactory.getInstance().setWriter(other.getElementType().lub(getElementType()));
		Set o = (Set) other;
		
		for(IValue v : content){
			if(o.content.contains(v)){
				w.insert(v);
			}
		}
		
		return w.done();
	}

	@SuppressWarnings("unchecked")
	public ISet subtract(ISet other) {
		ISetWriter sw = ValueFactory.getInstance().setWriter(getElementType());
		for (IValue a : content){
			if (!other.contains(a)){
				sw.insert(a);
			}
		}
		return sw.done();
	}

	public boolean isSubSet(ISet other) {
		for (IValue elem : this) {
			if (!other.contains(elem)) {
				return false;
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public <SetOrRel extends ISet> SetOrRel union(ISet other){
		ISetWriter w = ValueFactory.getInstance().setWriter(other.getElementType().lub(getElementType()));
		w.insertAll(this);
		w.insertAll(other);
		ISet result = w.done();
		
		try{
			// either a set or a relation is returned. If the caller
			// expected a relation, but the union constructed a set
			// this will throw a ClassCastException
			return (SetOrRel) result;
		}
		catch (ClassCastException e) {
			throw new FactTypeError("Union did not result in a relation.", e);
		}
	}

	public Iterator<IValue> iterator(){
		return content.iterator();
	}

	public Type getElementType(){
		return ((SetType) fType.getBaseType()).getElementType();
	}
	
	public boolean equals(Object o){
		if (getClass() == o.getClass()) {
			Set other = (Set) o;
			
			return fType.comparable(other.fType) && content.equals(other.content);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return fAnnotations.hashCode() << 8 + fType.hashCode() + content.hashCode();
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append("{");
		
		Iterator<IValue> setIterator = iterator();
		if(setIterator.hasNext()){
			sb.append(setIterator.next());
			
			while(setIterator.hasNext()){
				sb.append(",");
				sb.append(setIterator.next());
			}
		}
		
		sb.append("}");
		
		return sb.toString();
	}

	public IRelation product(ISet set){
		TupleType resultType = TypeFactory.getInstance().tupleType(getElementType(),set.getElementType());
		IRelationWriter w = new Relation.RelationWriter(resultType);

		for(IValue t1 : this){
			for(IValue t2 : set){
				ITuple t3 = new Tuple(t1,t2);
				w.insert(t3);
			}
		}

		return w.done();
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitSet(this);
	}
	
	protected IValue clone(String label, IValue anno) {
		return new Set(this, label, anno);
	}
	
	private static void checkInsert(IValue elem, Type eltType) throws FactTypeError{
		Type type = elem.getType();
		if(!type.isSubtypeOf(eltType)){
			throw new FactTypeError("Element type " + type + " is not compatible with " + eltType);
		}
	}
	
	static ISetWriter createSetWriter(Type eltType){
		return new SetWriter(eltType);
	}
	
	protected static class SetWriter extends Writer implements ISetWriter{
		protected final Type eltType;
		protected final HashSet<IValue> setContent;
		
		protected Set constructedSet;

		public SetWriter(Type eltType){
			super();
			
			this.eltType = eltType;
			setContent = new HashSet<IValue>();
		}
		
		private void put(IValue elem){
			checkInsert(elem, eltType);
			setContent.add(elem);
		}

		public void insert(IValue... elems) throws FactTypeError{
			checkMutation();
			
			for(IValue elem : elems){
				put(elem);
			}
		}

		public void insertAll(Iterable<IValue> collection) throws FactTypeError{
			checkMutation();
			
			for(IValue v : collection){
				put(v);
			}
		}
		
		public ISet done(){
			if(constructedSet == null){
				constructedSet = new Set(eltType, setContent, fAnnotations);
			}
			return  constructedSet;
		}
		
		private void checkMutation(){
			if(constructedSet != null) throw new UnsupportedOperationException("Mutation of a finalized set is not supported.");
		}
		
		public int size() {
			return constructedSet.size();
		}
	}
}