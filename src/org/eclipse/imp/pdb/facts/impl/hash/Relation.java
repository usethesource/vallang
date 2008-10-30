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

import java.util.Collection;
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

class Relation extends WritableValue<IRelationWriter> implements IRelation {
	static class RelationWriter extends WriterBase<IRelationWriter> implements
			IRelationWriter {
		private Relation fRelation; // cached for convenience (avoids casts on every insert)

		public RelationWriter(Relation relation) {
			super(relation);
			fRelation = relation;
		}

		public IRelation getRelation() {
			return fRelation;
		}

		public void insert(ITuple tuple) throws FactTypeError {
			checkMutable();
			fRelation.checkInsert(tuple);
			fRelation.fTuples.add((Tuple) tuple);
		}

		public void insertAll(IRelation other) throws FactTypeError {
			checkMutable();
			fRelation.checkRelationType(other);
			fRelation.fTuples.addAll(((Relation) other).fTuples);
		}
		
		@SuppressWarnings("unchecked")
		public void insertAll(IList other) throws FactTypeError {
			checkMutable();
			fRelation.checkInsert(other.getElementType());
			// Perhaps a bug in javac 5.0 on MacOS X? The following cast fails with an
			// "inconvertible type" error, even though the type arguments get erased:
//                      fRelation.fTuples.addAll((Collection<? extends ITuple>) ((List) other).fList);
			fRelation.fTuples.addAll((Collection) ((List) other).fList);
		}

		public void insertAll(ISet set) throws FactTypeError {
			checkMutable();
			fRelation.checkSetType(set);
			
			for (IValue v : set) {
				ITuple t = (ITuple) v;
				fRelation.fTuples.add(t);
			}
		}
		
		public void insertAll(Iterable<? extends ITuple> collection) throws FactTypeError {
			for (ITuple v : collection) {
				insert(v);
			}
			
		}
	}

	/*package*/ java.util.HashSet<ITuple> fTuples = new HashSet<ITuple>();

	/* package */Relation(NamedType relType) throws FactTypeError {
		super(relType);
		Type baseType = relType.getBaseType();
		if (!baseType.isRelationType()) {
			throw new FactTypeError("This is not a relation type:" + relType);
		}
	}

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
	
	protected IRelationWriter createWriter() {
		return new RelationWriter(this);
	}

	protected Relation createRelationOfSameType() {
		if (fType.isNamedType()) {
			try {
				return new Relation((NamedType) fType);
			} catch (FactTypeError e) {
				// will never happen because fType has been typechecked before.
				return null;
			}
		} else {
			return new Relation((RelationType) fType);
		}
	}

	public IRelation product(IRelation r) {
		RelationType newRelType = ((RelationType) getType().getBaseType()).product((RelationType) r.getType().getBaseType());
		int width = newRelType.getArity();
		Relation result = new Relation(newRelType);
		IRelationWriter w = result.getWriter();

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
		
		w.done();

		return result;
	}
	
	public IRelation product(ISet set) {
		TupleType singleton = TypeFactory.getInstance().tupleTypeOf(set.getElementType());
		RelationType resultType = ((RelationType) getType().getBaseType()).product(singleton);
		IRelation result = new Relation(resultType);
		int width = resultType.getArity();
		IRelationWriter w = result.getWriter();

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
		
		w.done();

		return result;
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
		Type resultType = ((RelationType) getType().getBaseType()).compose((RelationType) other.getType().getBaseType());
		IRelation result = new Relation(resultType);
		IRelationWriter w = result.getWriter();
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
		w.done();
		return result;
	}

	public boolean contains(ITuple tuple) throws FactTypeError {
		checkInsert(tuple);
		return fTuples.contains(tuple);
	}

	public IRelation insert(ITuple tuple) throws FactTypeError {
		Type newType = checkInsert(tuple);
		Relation newRel = new Relation(newType);
		newRel.fTuples.addAll(fTuples);
		newRel.fTuples.add(tuple);
		newRel.getWriter().done();
		return newRel;
	}

	

	public IRelation intersect(IRelation o) throws FactTypeError {
		Type newType = checkRelationType(o);
		Relation newRel = new Relation(newType);
		Relation other = (Relation) o;

		for (ITuple t : this) {
			if (other.fTuples.contains(t)) {
				newRel.fTuples.add(t);
			}
		}

		newRel.getWriter().done();
		return newRel;
	}

	public IRelation intersect(ISet set) throws FactTypeError {
		RelationType newType = checkSetType(set);

		Relation newRel = new Relation((RelationType) newType);
		Set other = (Set) set;

		for (ITuple t : this) {
			if (other.fSet.contains(t)) {
				newRel.fTuples.add(t);
			}
		}

		newRel.getWriter().done();
		return newRel;
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
		Type newType = checkRelationType(o);

		Relation newRel = new Relation(newType);
		Relation other = (Relation) o;

		for (ITuple t : fTuples) {
			if (!other.fTuples.contains(t)) {
				newRel.fTuples.add(t);
			}
		}

		newRel.getWriter().done();

		return newRel;
	}

	public IRelation subtract(ISet set) throws FactTypeError {
		RelationType newType = checkSetType(set);
		Relation newRel = new Relation(newType);

		for (ITuple t : fTuples) {
			if (!set.contains(t)) {
				newRel.fTuples.add(t);
			}
		}

		newRel.getWriter().done();

		return newRel;
	}

	public IRelation union(IRelation other) throws FactTypeError {
		Type newType = checkRelationType(other);
		IRelation newRel = new Relation(newType);
		IRelationWriter w = newRel.getWriter();

		for (ITuple t : fTuples) {
			w.insert(t);
		}
		for (Iterator<ITuple> iter = other.iterator(); iter.hasNext();) {
			ITuple tuple = iter.next();
			w.insert(tuple);
		}
		w.done();
		return newRel;
	}

	public IRelation union(ISet set) throws FactTypeError {
		RelationType newType = checkSetType(set);
		IRelation newRel = new Relation(newType);
		IRelationWriter w = newRel.getWriter();

		for (ITuple t : fTuples) {
			w.insert(t);
		}
		for (Iterator<IValue> iter = set.iterator(); iter.hasNext();) {
			ITuple tuple = (ITuple) iter.next();
			w.insert(tuple);
		}
		w.done();
		return newRel;
	}

	public Iterator<ITuple> iterator() {
		return this.fTuples.iterator();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{ ");
		int idx = 0;
		for (ITuple tuple : fTuples) {
			if (idx++ > 0)
				sb.append(",\n  ");
			sb.append(tuple.toString());
		}
		sb.append("\n}");
		return sb.toString();
	}

	public ISet toSet() {
		Set result = new Set(((RelationType) getType().getBaseType()).toSet());
		ISetWriter w = result.getWriter();
		try {
			w.insertAll((IRelation) this);
		} catch (FactTypeError e) {
			// this will not happen
		}
		w.done();
		return result;
	}

	public IList topologicalOrderedList() throws FactTypeError {
		checkReflexivity();
		// TODO implement topo ordering
		throw new UnsupportedOperationException("NYI");
	}
	
	public ISet carrier() {
		SetType newType = checkCarrier();
		Set result = new Set(newType);
		ISetWriter w = result.getWriter();
		
		try {
			for (ITuple t : this) {
				for (IValue v : t) {
					w.insert(v);
				}
			}
		} catch (FactTypeError e) {
			// this will not happen
		}
		w.done();
		return result;
	}

	
	private Type checkRelationType(IRelation other)
			throws FactTypeError {
		Type otherType = other.getType();
		return checkRelationType(otherType);
	}

	private Type checkRelationType(Type otherType) throws FactTypeError {
		Type newType = getType().lub(otherType);
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

	private Type checkInsert(ITuple tuple) throws FactTypeError {
		return checkInsert(tuple.getType());
	}
	
	private Type checkInsert(Type type) throws FactTypeError {
		if (!((RelationType) getType().getBaseType()).acceptsElementOf(type)) {
			throw new FactTypeError("Type " + type + " is not compatible for insert into " + getType());
		}
		return getType();
	}

	private SetType checkCarrier() {
		RelationType type = (RelationType) getType().getBaseType();
		Type result = type.getFieldType(0);
		int width = type.getArity();
		
		for (int i = 1; i < width; i++) {
			result = result.lub(type.getFieldType(i));
		}
		
		return TypeFactory.getInstance().setTypeOf(result);
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
	
	public IValue accept(IValueVisitor v) throws VisitorException {
		return v.visitRelation(this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Object clone() throws CloneNotSupportedException {
		Relation tmp;
		
		if (getType() instanceof NamedType) {
		    tmp =  new Relation((NamedType) getType());
		}
		else {
			tmp = new Relation((RelationType) getType());
		}
	
		// we don't have to clone fList if this instance is not mutable anymore,
		// otherwise we certainly do, to prevent modification of the original list.
		if (isMutable()) {
			tmp.fTuples = (HashSet<ITuple>) fTuples.clone();
		}
		else {
			tmp.fTuples = fTuples;
			tmp.getWriter().done();
		}
		
		return tmp;
	}

	public TupleType getFieldTypes() {
		return ((RelationType) fType).getFieldTypes();
	}
}
