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

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.WritableValue;
import org.eclipse.imp.pdb.facts.impl.WriterBase;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.RelationType;
import org.eclipse.imp.pdb.facts.type.SetType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Set extends WritableValue<ISetWriter> implements ISet {
	static class SetWriter extends WriterBase<ISetWriter> implements ISetWriter {
		private Set fSet; // cached for convenience (avoids casts on every

		// insert)

		public SetWriter(Set set) {
			super(set);
			fSet = set;
		}

		public ISet getSet() {
			return fSet;
		}

		public void insert(IValue v) throws FactTypeError {
			checkMutable();
			fSet.checkInsert(v);
			fSet.fSet.add(v);
		}

		public void insertAll(ISet other) throws FactTypeError {
			checkMutable();
			fSet.checkInsert(other.getElementType());
			fSet.fSet.addAll(((Set) other).fSet);
		}

		public void insertAll(IRelation relation) throws FactTypeError {
			checkMutable();
			SetType setType = ((RelationType) relation.getType().getBaseType()).toSet();
			fSet.checkInsert(setType.getElementType());
			fSet.fSet.addAll(((Relation) relation).fTuples);
		}

		public void insertAll(IList list) throws FactTypeError {
			checkMutable();
			fSet.checkInsert(list.getElementType());
			fSet.fSet.addAll(((List) list).fList);
		}
		
		public void insertAll(Iterable<? extends IValue> collection) throws FactTypeError {
			checkMutable();
			for (IValue v : collection) {
				insert(v);
			}
		}
	}

	HashSet<IValue> fSet = new HashSet<IValue>();

	/* package */Set(NamedType setType) throws FactTypeError {
		super(setType);
		if (!setType.getBaseType().isSetType()) {
			throw new FactTypeError("named type is not a set:" + setType);
		}
	}

	/* package */Set(SetType setType) {
		super(setType);
	}
	
	/* package */Set(Type eltType) {
		super(TypeFactory.getInstance().setTypeOf(eltType));
	}

	@Override
	protected ISetWriter createWriter() {
		return new SetWriter(this);
	}

	public ISet insert(IValue element) throws FactTypeError {
		SetType newType = checkInsert(element);
		Set result = new Set(newType);
		ISetWriter sw = result.getWriter();
		sw.insertAll(this);
		sw.insert(element);
		sw.done();

		return result;
	}

	public boolean contains(IValue element) throws FactTypeError {
		checkInsert(element);
		return fSet.contains(element);
	}

	public ISet intersect(ISet other) throws FactTypeError {
		SetType newType = checkSet(other);
		ISet result = new Set(newType);
		ISetWriter w = result.getWriter();
		Set o = (Set) other;
		
		for (IValue v : fSet) {
			if (o.fSet.contains(v)) {
				w.insert(v);
			}
		}
		
		w.done();
		
		return result;
	}

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

	public ISet subtract(ISet other) throws FactTypeError {
		SetType newType = checkSet(other);
		ISet result = new Set(newType);
		ISetWriter sw = result.getWriter();
		for (IValue a : fSet) {
			if (!other.contains(a)) {
				sw.insert(a);
			}
		}
		sw.done();
		return result;
	}

	public ISet union(ISet other) {
		SetType newType = checkSet(other);
		Set result = new Set(newType);
		ISetWriter w = result.getWriter();

		try {
			for (IValue a : this) {
				w.insert(a);
			}
			for (IValue a : other) {
				w.insert(a);
			}
		} catch (FactTypeError e) {
			// this can not happen
		}
		
		w.done();
		
		return result;
	}

	public Iterator<IValue> iterator() {
		return fSet.iterator();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{ ");
		int idx = 0;
		for (IValue a : this) {
			if (idx++ > 0)
				sb.append(",\n  ");
			sb.append(a.toString());
		}
		sb.append("\n}");
		return sb.toString();
	}

	public IRelation toRelation() throws FactTypeError {
		if (!getElementType().isTupleType()) {
			throw new FactTypeError("toRelation: element type is not tuple");
		}
		
		TupleType t = (TupleType) ((SetType) getType().getBaseType()).getElementType();
		Relation result = new Relation(t);
		IRelationWriter w = result.getWriter();
		try {
			w.insertAll(this);
		} catch (FactTypeError e) {
			// this will not happen
		}
		w.done();
		return result;
	}

	public Type getElementType() {
		return ((SetType) fType.getBaseType()).getElementType();
	}

	public IRelation product(IRelation r) {
		TypeFactory f = TypeFactory.getInstance();
		RelationType t1 = f.relType(f.tupleTypeOf(getElementType()));
		RelationType relType = t1.product((RelationType) r.getBaseType());
		IRelation result = new Relation(relType);
		int width = relType.getArity();
		IRelationWriter w= result.getWriter();

		try {
			for (IValue v1 : this) {
				for (ITuple t2 : r) {
					IValue[] e2 = ((Tuple) t2).fElements;
					IValue[] e3 = new IValue[width];

					e3[0] = v1;
					
					for (int i = 1; i < width; i++) {
						e3[i] = e2[i - 1];
					}

					ITuple t3 = new Tuple(e3);

					w.insert(t3);

				}
			}
		} catch (FactTypeError e) {
			// does not happen since we have constructed the correct types
			// above.
		}
		
		w.done();

		return result;
	}
	
	public IRelation product(ISet set) {
		TupleType resultType = TypeFactory.getInstance().tupleTypeOf(getElementType(), set.getElementType());
		IRelation result = new Relation(resultType);
		IRelationWriter w = result.getWriter();

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
		
		w.done();

		return result;
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
		
		return TypeFactory.getInstance().setTypeOf(eltType);
	}
	
	private RelationType checkRelation(IRelation o) throws FactTypeError {
		SetType t = (SetType) getType().getBaseType();
		RelationType newType = (RelationType) t.lub(o.getType());
		
		if (!newType.getBaseType().isRelationType()) {
			throw new FactTypeError("relation type " + o.getType() + " is not compatible with this set's type: " + getType());
		}
		
		return newType;
	}
	
	@Override
	public boolean equals(Object o) {
		// TODO: should we allow ISet here?
		// TODO: should the types be equal?
		if (!(o instanceof Set)) {
			return false;
		}
		
		Set other = (Set) o;
		
		return fSet.equals(other.fSet);
	}

	public IRelation intersect(IRelation rel) throws FactTypeError {
		return rel.intersect(this);
	}

	public IRelation invert(IRelation universe) throws FactTypeError {
		IRelation result = universe.subtract(this);
		
		if (size() + result.size() != universe.size()) {
			throw new FactTypeError("Universe is a not a superset of this set");
		}
		
		return result;
	}

	public IRelation subtract(IRelation rel) throws FactTypeError {
		Type newType = checkRelation(rel);
		IRelation result = new Relation(newType);
		IRelationWriter sw = result.getWriter();
		for (IValue a : fSet) {
			ITuple t = (ITuple) a;
			if (!rel.contains(t)) {
				sw.insert(t);
			}
		}
		sw.done();
		return result;
	}

	public IRelation union(IRelation rel) throws FactTypeError {
		return rel.union(this);
	}
	
	public IValue accept(IValueVisitor v) throws VisitorException {
		return v.visitSet(this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Set tmp;
		
		if (getType() instanceof NamedType) {
		    tmp =  new Set((NamedType) getType());
		}
		else {
			tmp = new Set((SetType) getType());
		}
	
		// we don't have to clone fList if this instance is not mutable anymore,
		// otherwise we certainly do, to prevent modification of the original list.
		if (isMutable()) {
			tmp.fSet = (HashSet<IValue>) fSet.clone();
		}
		else {
			tmp.fSet = fSet;
			tmp.getWriter().done();
		}
		
		return tmp;
	}
}
