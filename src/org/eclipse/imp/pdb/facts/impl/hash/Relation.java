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

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Relation extends Set implements IRelation {

	/* package */Relation(Type type, HashSet<IValue> content) {
		super(TypeFactory.getInstance().relTypeFromTuple(type), content);
	}
	
	private Relation(Relation other, String label, IValue anno) {
		super(other, label, anno);
	}

	public int arity() {
		return getType().getArity();
	}
	
	@Override
	public IRelation insert(IValue element) throws FactTypeError {
		// class cast exception can not occur because superclass
		// will construct a relation.
		return (IRelation) super.insert(element);
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
	
	public IRelation closureStar() throws FactTypeError {
		checkReflexivity();
		IRelation closure = closure();
		ISet carrier = carrier();
		Type elementType = carrier.getElementType();
		ISetWriter reflex = Set.createSetWriter(TypeFactory.getInstance().tupleType(elementType, elementType));
		
		for (IValue e: carrier) {
			reflex.insert(new Tuple(new IValue[] {e, e}));
		}
		
		return closure.union(reflex.done());
	}

	public IRelation compose(IRelation other) throws FactTypeError {
		Type resultType = getType().compose(other.getType());
		IRelationWriter w = ValueFactory.getInstance().relationWriter(resultType.getFieldTypes());
		int max1 = arity() - 1;
		int max2 = other.arity() - 1;
		int width = max1 + max2;

		for (IValue v1 : content) {
			ITuple t1 = (ITuple) v1;
			for (IValue t2 : other) {
				IValue[] values = new IValue[width];
				ITuple tuple2 = (ITuple) t2;
				if (t1.get(max1).equals(tuple2.get(0))) {
					for (int i = 0; i < max1; i++) {
						values[i] = t1.get(i);
					}

					for (int i = max1, j = 1; i < width; i++, j++) {
						values[i] = tuple2.get(j);
					}

					w.insert(new Tuple(values));
				}
			}
		}
		return w.done();
	}

	public ISet carrier() {
		Type newType = checkCarrier();
		ISetWriter w = Set.createSetWriter(newType.getElementType());
		
		for (IValue t : this) {
			w.insertAll((ITuple) t);
		}
		return w.done();
	}

	public ISet domain() {
		Type relType = getType();
		ISetWriter w = Set.createSetWriter(relType.getFieldType(0));
		
		for (IValue elem : this) {
			ITuple tuple = (ITuple) elem;
			w.insert(tuple.get(0));
		}
		return w.done();
	}
	
	public ISet range() {
		Type relType = getType();
		int last = relType.getArity() - 1;
		ISetWriter w = Set.createSetWriter(relType.getFieldType(last));
		
		for (IValue elem : this) {
			ITuple tuple = (ITuple) elem;
			w.insert(tuple.get(last));
		}
		
		return w.done();
	}
	
	private boolean checkReflexivity() throws FactTypeError {
		Type type = getType();
		
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
		if (!t1.comparable(t2)) {
			throw new FactTypeError(
					"Elements of binary relations are not subtypes of eachother:"
							+ t1 + " and " + t2);
		}
	}

	private Type checkCarrier() {
		Type result = fType.getFieldType(0);
		int width = fType.getArity();
		
		for (int i = 1; i < width; i++) {
			result = result.lub(fType.getFieldType(i));
		}
		
		return TypeFactory.getInstance().setType(result);
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitRelation(this);
	}
	
	public Type getFieldTypes() {
		return fType.getFieldTypes();
	}
	
	@Override
	protected IValue clone(String label, IValue anno)  {
		return new Relation(this, label, anno);
	}

	public static IRelationWriter createRelationWriter(Type tupleType) {
		return new RelationWriter(tupleType);
	}
	
	protected static class RelationWriter  extends Set.SetWriter implements IRelationWriter {
		public RelationWriter(Type eltType) {
			super(eltType);
		}
			
		public IRelation done() {
			if(constructedSet == null){
				constructedSet = new Relation(eltType, setContent);
			}
			return  (IRelation) constructedSet;
		}
	}
	
	public ISet select(int... fields) {
		Type eltType = getFieldTypes().select(fields);
		ISetWriter w = ValueFactory.getInstance().setWriter(eltType);
		
		for (IValue v : this) {
			w.insert(((ITuple) v).select(fields));
		}
		
		return w.done();
	}
	
	public ISet select(String... fields) {
		int[] indexes = new int[fields.length];
		int i = 0;
		
		if (getFieldTypes().hasFieldNames()) {
			for (String field : fields) {
				indexes[i++] = getFieldTypes().getFieldIndex(field);
			}
			
			return select(indexes);
		}
		else {
			throw new FactTypeError("Relation does not have field names");
		}
	}
}