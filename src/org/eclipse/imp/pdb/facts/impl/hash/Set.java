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

import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.SetType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Set extends Value implements ISet {
	static class SetWriter  implements ISetWriter {
		protected final ISet fSet; 

		public SetWriter(Type eltType) {
			if (eltType.getBaseType().isTupleType()) {
				fSet = new Relation((TupleType) eltType.getBaseType());
			}
			else {
			  fSet = new Set(eltType);
			}
		}

		public void insert(IValue... elems) throws FactTypeError {
			for (IValue v : elems) {
			  ((Set)fSet).checkInsert(v);
			  ((Set)fSet).fSet.add(v);
			}
		}

		public void insertAll(Iterable<? extends IValue> collection) throws FactTypeError {
			for (IValue v : collection) {
				insert(v);
			}
		}
		
		@SuppressWarnings("unchecked")
		public <SetOrRel extends ISet> SetOrRel done() {
			try {
			  return (SetOrRel) fSet;
			}
			catch (ClassCastException e) {
				throw new FactTypeError("done() returns a set or a relation");
			}
		}
	}

	final HashSet<IValue> fSet;

	/* package */Set(SetType setType) {
		super(setType);
		fSet = new HashSet<IValue>();
	}
	
	/* package */Set(Type eltType) {
		this(TypeFactory.getInstance().setType(eltType));
	}
	
	protected Set(Set other) {
		super(other);
		fSet = other.fSet;
	}

	@SuppressWarnings("unchecked")
	public ISet insert(IValue element) throws FactTypeError {
		SetType newType = checkInsert(element);
		ISetWriter sw = new SetWriter(newType.getElementType());
		sw.insertAll(this);
		sw.insert(element);
		return sw.done();
	}

	public boolean contains(IValue element) throws FactTypeError {
		checkInsert(element);
		return fSet.contains(element);
	}

	@SuppressWarnings("unchecked")
	public ISet intersect(ISet other) throws FactTypeError {
		SetType newType = checkSet(other);
		ISetWriter w = new SetWriter(newType.getElementType());
		Set o = (Set) other;
		
		for (IValue v : fSet) {
			if (o.fSet.contains(v)) {
				w.insert(v);
			}
		}
		
		return w.done();
	}

	@SuppressWarnings("unchecked")
	public ISet invert(ISet universe) throws FactTypeError {
		ISet result = universe.subtract(this);
		
		if (size() + result.size() != universe.size()) {
			throw new FactTypeError("Universe is not a superset of this set");
		}
		
		return result;
	}

	public boolean isEmpty() {
		return fSet.isEmpty();
	}

	public int size() {
		return fSet.size();
	}

	@SuppressWarnings("unchecked")
	public ISet subtract(ISet other) throws FactTypeError {
		SetType newType = checkSet(other);
		ISetWriter sw = new SetWriter(newType.getElementType());
		for (IValue a : fSet) {
			if (!other.contains(a)) {
				sw.insert(a);
			}
		}
		return sw.done();
	}

	@SuppressWarnings("unchecked")
	public <SetOrRel extends ISet> SetOrRel union(ISet other) {
		SetType newType = checkSet(other);
		ISetWriter w = new SetWriter(newType.getElementType());

		try {
			w.insertAll(this);
			w.insertAll(other);
		} catch (FactTypeError e) {
			// this can not happen
		}
		
		try {
			// either a set or a relation is returned. If the caller
			// expected a relation, but the union constructed a set
			// this will throw a ClassCastException
			return (SetOrRel) w.done();
		}
		catch (ClassCastException e) {
			throw new FactTypeError("Union did not result in a relation.", e);
		}
	}

	public Iterator<IValue> iterator() {
		return fSet.iterator();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		int idx = 0;
		for (IValue a : this) {
			if (idx++ > 0)
				sb.append(",");
			sb.append(a.toString());
		}
		sb.append("}");
		return sb.toString();
	}

	public Type getElementType() {
		return ((SetType) fType.getBaseType()).getElementType();
	}

	public IRelation product(ISet set) {
		TupleType resultType = TypeFactory.getInstance().tupleType(getElementType(),set.getElementType());
		ISetWriter w = new SetWriter(resultType);

		try {
			for (IValue t1 : this) {
				for (IValue t2 : set) {
					ITuple t3 = new Tuple(t1,t2);
					w.insert(t3);
				}
			}
		} catch (FactTypeError e) {
			// does not happen since we have constructed the correct types
			// above.
		}
		
		return (IRelation) w.done();
	}
	
	private SetType checkInsert(IValue element) throws FactTypeError {
		checkInsert(element.getType());
		return (SetType) getType();
	}
	
	private void checkInsert(Type eltType) throws FactTypeError {
		if (!eltType.isSubtypeOf(getElementType())) {
			throw new FactTypeError("Element type " + eltType + " is not a subtype of " + getElementType());
		}
	}
	
	private SetType checkSet(ISet other) {
		SetType t = (SetType) getType().getBaseType();
		SetType ot = (SetType) other.getType().getBaseType();
		Type eltType = t.getElementType().lub(ot.getElementType());
		
		return TypeFactory.getInstance().setType(eltType);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Set)) {
			return false;
		}
		
		Set other = (Set) o;
		
		return fSet.equals(other.fSet);
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitSet(this);
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new Set(this);
	}
}
