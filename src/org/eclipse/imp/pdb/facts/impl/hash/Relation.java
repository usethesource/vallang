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
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.RelationType;
import org.eclipse.imp.pdb.facts.type.SetType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Relation extends Value implements IRelation {
	static class RelationWriter implements IRelationWriter {
		private final Relation fRelation;

		public RelationWriter(TupleType elements) {
			fRelation = new Relation(elements);
		}

		public void insert(IValue... tuples) throws FactTypeError {
			for (IValue e : tuples) {
			  fRelation.checkInsert((ITuple) e);
			  fRelation.fTuples.add((Tuple) e);
			}
		}

		public void insertAll(Iterable<? extends ITuple> collection) throws FactTypeError {
			for (ITuple v : collection) {
				insert(v);
			}
			
		}
		
		public IRelation done() {
			return fRelation;
		}
	}

	/*package*/ java.util.HashSet<ITuple> fTuples = new HashSet<ITuple>();

	/* package */Relation(RelationType relType) {
		super(relType);
	}
	
	/*package*/ Relation(Type relType) {
		super(relType);
	}

	/* package */Relation(TupleType type) {
		super(TypeFactory.getInstance().relType(type));
	}

	public boolean isEmpty() {
		return fTuples.isEmpty();
	}

	public int size() {
		return fTuples.size();
	}

	public int arity() {
		return ((RelationType) getType().getBaseType()).getArity();
	}
	
	public IRelation product(IRelation r) {
		RelationType newRelType = ((RelationType) getType().getBaseType()).product((RelationType) r.getType().getBaseType());
		int width = newRelType.getArity();
		IRelationWriter w = new RelationWriter(newRelType.getFieldTypes());

		try {
			for (ITuple t1 : fTuples) {
				IValue[] e1 = ((Tuple) t1).fElements;
				for (ITuple t2 : r) {
					IValue[] e2 = ((Tuple) t2).fElements;
					IValue[] e3 = new IValue[width];

					for (int i = 0; i < e1.length; i++) {
						e3[i] = e1[i];
					}
					for (int i = e1.length, j = 0; i < width; i++, j++) {
						e3[i] = e2[j];
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
		TupleType singleton = TypeFactory.getInstance().tupleType(set.getElementType());
		RelationType resultType = ((RelationType) getType().getBaseType()).product(singleton);
		int width = resultType.getArity();
		IRelationWriter w = new RelationWriter(resultType.getFieldTypes());
		try {
			for (ITuple t1 : fTuples) {
				IValue[] e1 = ((Tuple) t1).fElements;
				for (IValue t2 : set) {
					IValue[] e3 = new IValue[width];

					for (int i = 0; i < e1.length; i++) {
						e3[i] = e1[i];
					}
					
					e3[e1.length] = t2;

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

	public IRelation closure() throws FactTypeError {
		checkReflexivity();
		IRelation tmp = (IRelation) this;

		int prevCount = 0;

		while (prevCount != tmp.size()) {
			prevCount = tmp.size();
			tmp = (IRelation) tmp.union(tmp.compose(tmp));
		}

		return tmp;
	}

	public IRelation compose(IRelation other) throws FactTypeError {
		RelationType resultType = ((RelationType) getType().getBaseType()).compose((RelationType) other.getType().getBaseType());
		IRelationWriter w = new RelationWriter(resultType.getFieldTypes());
		int max1 = ((RelationType) getType().getBaseType()).getArity() - 1;
		int max2 = ((RelationType) other.getType().getBaseType()).getArity() - 1;
		int width = max1 + max2;

		for (ITuple t1 : fTuples) {
			for (ITuple t2 : other) {
				IValue[] values = new IValue[width];

				if (t1.get(max1).equals(t2.get(0))) {
					for (int i = 0; i < max1; i++) {
						values[i] = t1.get(i);
					}

					for (int i = max1, j = 1; i < width; i++, j++) {
						values[i] = t2.get(j);
					}

					w.insert(new Tuple(values));
				}
			}
		}
		return w.done();
	}

	public boolean contains(ITuple tuple) throws FactTypeError {
		checkInsert(tuple);
		return fTuples.contains(tuple);
	}

	public IRelation insert(ITuple tuple) throws FactTypeError {
		TupleType newType = checkInsert(tuple);
		IRelationWriter w = new RelationWriter(newType);
		w.insertAll(fTuples);
		w.insert(tuple);
		return w.done();
	}

	

	public IRelation intersect(IRelation o) throws FactTypeError {
		RelationType newType = checkRelationType(o);
		IRelationWriter w = new RelationWriter(newType.getFieldTypes());
		Relation other = (Relation) o;

		for (ITuple t : this) {
			if (other.fTuples.contains(t)) {
				w.insert(t);
			}
		}

		return w.done();
	}

	public IRelation intersect(ISet set) throws FactTypeError {
		RelationType newType = checkSetType(set);
		IRelationWriter w = new RelationWriter(newType.getFieldTypes());
		Set other = (Set) set;

		for (ITuple t : this) {
			if (other.fSet.contains(t)) {
				w.insert(t);
			}
		}

		return w.done();
	}

	public IRelation invert(IRelation universe) throws FactTypeError {
		IRelation result = universe.subtract(this);
		
		if (size() + result.size() != universe.size()) {
			throw new FactTypeError("Universe is a not a superset of this relation");
		}
		
		return result;
	}

	public IRelation invert(ISet universe) throws FactTypeError {
		IRelation result = universe.subtract(this.toSet()).toRelation();
		
		if (size() + result.size() != universe.size()) {
			throw new FactTypeError("Universe is a not a superset of this relation");
		}
		
		return result;
	}

	public IRelation subtract(IRelation o) throws FactTypeError {
		RelationType newType = checkRelationType(o);

		IRelationWriter w = new RelationWriter(newType.getFieldTypes());
		Relation other = (Relation) o;

		for (ITuple t : fTuples) {
			if (!other.fTuples.contains(t)) {
				w.insert(t);
			}
		}

		return w.done();
	}

	public IRelation subtract(ISet set) throws FactTypeError {
		RelationType newType = checkSetType(set);
		IRelationWriter newRel = new RelationWriter(newType.getFieldTypes());

		for (ITuple t : fTuples) {
			if (!set.contains(t)) {
				newRel.insert(t);
			}
		}

		return newRel.done();
	}

	public IRelation union(IRelation other) throws FactTypeError {
		RelationType newType = checkRelationType(other);
		IRelationWriter w = new RelationWriter(newType.getFieldTypes());

		w.insertAll(this);
		w.insertAll(other);
		return w.done();
	}

	@SuppressWarnings("unchecked")
	public IRelation union(ISet set) throws FactTypeError {
		RelationType newType = checkSetType(set);
		IRelationWriter w = new RelationWriter(newType.getFieldTypes());

		w.insertAll(this);
		w.insertAll((Iterable<? extends ITuple>) set);
		return w.done();
	}

	public Iterator<ITuple> iterator() {
		return this.fTuples.iterator();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		int idx = 0;
		for (ITuple tuple : fTuples) {
			if (idx++ > 0)
				sb.append(",");
			sb.append(tuple.toString());
		}
		sb.append("}");
		return sb.toString();
	}

	public ISet toSet() {
		ISetWriter w = new Set.SetWriter(getFieldTypes());
		try {
			w.insertAll(this);
		} catch (FactTypeError e) {
			// this will not happen
		}
		return w.done();
	}

	public IList topologicalOrderedList() throws FactTypeError {
		checkReflexivity();
		// TODO implement topo ordering
		throw new UnsupportedOperationException("NYI");
	}
	
	public ISet carrier() {
		SetType newType = checkCarrier();
		ISetWriter w = new Set.SetWriter(newType.getElementType());
		
		try {
			for (ITuple t : this) {
				w.insertAll(t);
			}
		} catch (FactTypeError e) {
			// this will not happen
		}
		return w.done();
	}

	
	private RelationType checkRelationType(IRelation other)
			throws FactTypeError {
		Type otherType = other.getType();
		return checkRelationType(otherType);
	}

	private RelationType checkRelationType(Type otherType) throws FactTypeError {
		RelationType newType = (RelationType) getType().lub(otherType);
		Type baseType = newType.getBaseType();

		if (!baseType.isRelationType()) {
			throw new FactTypeError("Relation type is not compatible"
					+ otherType);
		}

		return newType;
	}

	private RelationType checkSetType(ISet other) throws FactTypeError {
		Type newType = getType().lub(other.getType());
		Type baseType = newType.getBaseType();

		if (!baseType.isRelationType()) {
			throw new FactTypeError("Set type " + other.getType()
					+ " is not compatible with " + getType());
		}

		return (RelationType) newType;
	}

	private boolean checkReflexivity() throws FactTypeError {
		RelationType type = (RelationType) getType().getBaseType();
		
		if (type.getArity() == 2) {
			Type t1 = type.getFieldType(0);
			Type t2 = type.getFieldType(1);

			checkSubtypesOfEachother(t1, t2);
		} else {
			throw new FactTypeError("Relation is not binary");
		}
		
		return true;
	}

	private void checkSubtypesOfEachother(Type t1, Type t2)
			throws FactTypeError {
		if (!t1.isSubtypeOf(t2) && !t1.isSubtypeOf(t2)) {
			throw new FactTypeError(
					"Elements of binary relations are not subtypes of eachother:"
							+ t1 + " and " + t2);
		}
	}

	private TupleType checkInsert(ITuple tuple) throws FactTypeError {
		return checkInsert((TupleType) tuple.getType());
	}
	
	private TupleType checkInsert(TupleType type) throws FactTypeError {
		if (!((RelationType) getType().getBaseType()).acceptsElementOf(type)) {
			throw new FactTypeError("Type " + type + " is not compatible for insert into " + getType());
		}
		return type;
	}

	private SetType checkCarrier() {
		RelationType type = (RelationType) getType().getBaseType();
		Type result = type.getFieldType(0);
		int width = type.getArity();
		
		for (int i = 1; i < width; i++) {
			result = result.lub(type.getFieldType(i));
		}
		
		return TypeFactory.getInstance().setType(result);
	}
	
	@Override
	public boolean equals(Object o) {
		// TODO: should we allow IRelation here?
		// TODO: should the types be equal?
		if (!(o instanceof Relation)) {
			return false;
		}
		Relation other = (Relation) o;
		
		return fTuples.equals(other.fTuples);
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitRelation(this);
	}
	
	@Override
	protected Object clone() throws CloneNotSupportedException {
		RelationWriter w = new RelationWriter(getFieldTypes());
		w.insertAll(fTuples);
		return w.done();
	}

	public TupleType getFieldTypes() {
		return ((RelationType) fType).getFieldTypes();
	}
}
