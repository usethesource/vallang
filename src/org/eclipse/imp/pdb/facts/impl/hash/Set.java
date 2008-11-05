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
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.RelationType;
import org.eclipse.imp.pdb.facts.type.SetType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Set extends Value implements ISet {
	static class SetWriter  implements ISetWriter {
		private final Set fSet; 

		public SetWriter(Type eltType) {
			fSet = new Set(eltType);
		}

		public void insert(IValue... elems) throws FactTypeError {
			for (IValue v : elems) {
			  fSet.checkInsert(v);
			  fSet.fSet.add(v);
			}
		}

		public void insertAll(Iterable<? extends IValue> collection) throws FactTypeError {
			for (IValue v : collection) {
				insert(v);
			}
		}
		
		public ISet done() {
			return fSet;
		}
	}

	HashSet<IValue> fSet = new HashSet<IValue>();

	/* package */Set(SetType setType) {
		super(setType);
	}
	
	/* package */Set(Type eltType) {
		super(TypeFactory.getInstance().setType(eltType));
	}

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
		ISetWriter sw = new SetWriter(newType.getElementType());
		for (IValue a : fSet) {
			if (!other.contains(a)) {
				sw.insert(a);
			}
		}
		return sw.done();
	}

	public ISet union(ISet other) {
		SetType newType = checkSet(other);
		ISetWriter w = new SetWriter(newType.getElementType());

		try {
			w.insertAll(this);
			w.insertAll(other);
		} catch (FactTypeError e) {
			// this can not happen
		}
		
		return w.done();
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

	@SuppressWarnings("unchecked")
	public IRelation toRelation() throws FactTypeError {
		if (!getElementType().isTupleType()) {
			throw new FactTypeError("toRelation: element type is not tuple");
		}
		
		TupleType t = (TupleType) ((SetType) getType().getBaseType()).getElementType();
		IRelationWriter w = new Relation.RelationWriter(t);
		try {
			w.insertAll((Iterable<? extends ITuple>) this);
		} catch (FactTypeError e) {
			// this will not happen
		}
		
		return w.done();
	}

	public Type getElementType() {
		return ((SetType) fType.getBaseType()).getElementType();
	}

	public IRelation product(IRelation r) {
		TypeFactory f = TypeFactory.getInstance();
		RelationType t1 = f.relType(f.tupleType(getElementType()));
		RelationType relType = t1.product((RelationType) r.getBaseType());
		IRelationWriter w = new Relation.RelationWriter(relType.getFieldTypes());
		int width = relType.getArity();

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
		
		return w.done();
	}
	
	public IRelation product(ISet set) {
		TupleType resultType = TypeFactory.getInstance().tupleType(getElementType(), set.getElementType());
		IRelationWriter w = new Relation.RelationWriter(resultType);

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
		
		return w.done();
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
		// TODO: should we allow IRelation here?
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
		RelationType newType = checkRelation(rel);
		IRelationWriter sw = new Relation.RelationWriter(newType.getFieldTypes());
		for (IValue a : fSet) {
			ITuple t = (ITuple) a;
			if (!rel.contains(t)) {
				sw.insert(t);
			}
		}
		return sw.done();
	}

	public IRelation union(IRelation rel) throws FactTypeError {
		return rel.union(this);
	}
	
	public IValue accept(IValueVisitor v) throws VisitorException {
		return v.visitSet(this);
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Set tmp = new Set((SetType) getType());
		tmp.fSet = fSet;
		return tmp;
	}
}
