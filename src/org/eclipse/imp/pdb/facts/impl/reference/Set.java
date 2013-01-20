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

package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
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

class Set extends Value implements ISet{
	final HashSet<IValue> content;
	private int fHash;

	/*package*/ Set(Type setType, HashSet<IValue> content){
		super(setType);
		
		this.content = content;
	}
	
	public boolean contains(IValue element) throws FactTypeUseException{
		return content.contains(element);
	}

	public boolean isEmpty(){
		return content.isEmpty();
	}

	public int size(){
		return content.size();
	}

	@SuppressWarnings("unchecked")
	public <SetOrRel extends ISet> SetOrRel insert(IValue element) {
		ISetWriter sw = ValueFactory.getInstance().setWriter(getElementType().lub(element.getType()));
		sw.insertAll(this);
		sw.insert(element);
		return (SetOrRel) sw.done();
	}

	@SuppressWarnings("unchecked")
	public <SetOrRel extends ISet> SetOrRel intersect(ISet other) {
		ISetWriter w = ValueFactory.getInstance().setWriter(other.getElementType().lub(getElementType()));
		Set o = (Set) other;
		
		for(IValue v : content){
			if(o.content.contains(v)){
				w.insert(v);
			}
		}
		
		return (SetOrRel) w.done();
	}

	@SuppressWarnings("unchecked")
	public <SetOrRel extends ISet> SetOrRel subtract(ISet other) {
		ISetWriter sw = ValueFactory.getInstance().setWriter(getElementType());
		for (IValue a : content){
			if (!other.contains(a)){
				sw.insert(a);
			}
		}
		return (SetOrRel) sw.done();
	}
	
	@SuppressWarnings("unchecked")
	public <SetOrRel extends ISet> SetOrRel delete(IValue elem) {
		ISetWriter sw = ValueFactory.getInstance().setWriter(getElementType());
		sw.insertAll(this);
		sw.delete(elem);
		return (SetOrRel) sw.done();
	}

	public boolean isSubsetOf(ISet other) {
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
		return (SetOrRel) result;
	}
	
	public Iterator<IValue> iterator(){
		return content.iterator();
	}

	public Type getElementType(){
		return fType.getElementType();
	}
	
	public boolean equals(Object o){
		  if(this == o) {
			  return true;
		  }
		  else if(o == null) {
			  return false;
		  }
		  else if (getClass() == o.getClass() || getClass() == o.getClass()) {
			Set other = (Set) o;
			
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

	public IRelation product(ISet set){
		Type resultType = TypeFactory.getInstance().tupleType(getElementType(),set.getElementType());
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
	
	private static void checkInsert(IValue elem, Type eltType) throws FactTypeUseException{
		Type type = elem.getType();
		if(!type.isSubtypeOf(eltType)){
			throw new UnexpectedElementTypeException(eltType, type);
		}
	}
	
	static ISetWriter createSetWriter(Type eltType){
		return new SetWriter(eltType);
	}
	
	static ISetWriter createSetWriter() {
		return new SetWriter();
	}
	
	protected static class SetWriter extends Writer implements ISetWriter{
		protected Type eltType;
		protected final HashSet<IValue> setContent;
		
		protected Set constructedSet;
		protected final boolean inferred;

		public SetWriter(Type eltType){
			super();
			
			this.eltType = eltType;
			this.inferred = false;
			setContent = new HashSet<IValue>();
		}
		
		public SetWriter() {
			super();
			this.eltType = TypeFactory.getInstance().voidType();
			this.inferred = true;
			setContent = new HashSet<IValue>();
		}
		
		private void put(IValue elem){
			updateType(elem);
			checkInsert(elem, eltType);
			setContent.add(elem);
		}

		private void updateType(IValue elem) {
			if (inferred) {
				eltType = eltType.lub(elem.getType());
			}
		}

		public void insert(IValue... elems) throws FactTypeUseException{
			checkMutation();
			
			for(IValue elem : elems){
				put(elem);
			}
		}

		public void insertAll(Iterable<? extends IValue> collection) throws FactTypeUseException{
			checkMutation();
			
			for(IValue v : collection){
				put(v);
			}
		}
		
		public ISet done(){
			if(constructedSet == null){
				if (inferred && eltType.isTupleType() || setContent.isEmpty()) {
					constructedSet = new Relation(setContent.isEmpty() ? TypeFactory.getInstance().voidType() : eltType, setContent);
				}
				else {
					constructedSet = new Set(TypeFactory.getInstance().setType(eltType), setContent);
				}
			}
			
			return  constructedSet;
		}
		
		private void checkMutation(){
			if(constructedSet != null) throw new UnsupportedOperationException("Mutation of a finalized set is not supported.");
		}
		
		public int size() {
			return constructedSet.size();
		}

		public void delete(IValue v) {
			checkMutation();
			setContent.remove(v);
		}
	}


}