/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Michael Steindorfer (Michael.Steindorfer@cwi.nl)
*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.reference;

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Relation extends Set implements IRelation {

	/* package */Relation(Type type, java.util.Set<IValue> content) {
		super(TypeFactory.getInstance().relTypeFromTuple(type), content);
	}
	
	public int arity() {
		return getType().getArity();
	}
	
	public IRelation closure() throws FactTypeUseException {
		getType().closure(); // will throw exception if not binary and reflexive
		IRelation tmp = this;

		int prevCount = 0;

		while (prevCount != tmp.size()) {
			prevCount = tmp.size();
			tmp = (IRelation) tmp.union(tmp.compose(tmp));
		}

		return tmp;
	}
	
	public IRelation closureStar() throws FactTypeUseException {
		Type resultType = getType().closure();
		// an exception will have been thrown if the type is not acceptable

		ISetWriter reflex = Relation.createRelationWriter(resultType.getElementType());

		for (IValue e: carrier()) {
			reflex.insert(new Tuple(new IValue[] {e, e}));
		}
		
		return closure().union(reflex.done());
	}

	public IRelation compose(IRelation other) throws FactTypeUseException {
		Type resultType = getType().compose(other.getType());
		// an exception will have been thrown if the relations are not both binary and
		// have a comparable field to compose.
		IRelationWriter w = ValueFactory.getInstance().relationWriter(resultType.getFieldTypes());

		for (IValue v1 : content) {
			ITuple tuple1 = (ITuple) v1;
			for (IValue t2 : other) {
				ITuple tuple2 = (ITuple) t2;
				
				if (tuple1.get(1).isEqual(tuple2.get(0))) {
					w.insert(new Tuple(tuple1.get(0), tuple2.get(1)));
				}
			}
		}
		return w.done();
	}

	public ISet carrier() {
		Type newType = getType().carrier();
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
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitRelation(this);
	}
	
	public Type getFieldTypes() {
		return fType.getFieldTypes();
	}
	
	public static IRelationWriter createRelationWriter(Type tupleType) {
		return new RelationWriter(tupleType);
	}
	
	public static IRelationWriter createRelationWriter() {
		return new RelationWriter();
	}
	
	protected static class RelationWriter  extends Set.SetWriter implements IRelationWriter {
		public RelationWriter(Type eltType) {
			super(eltType);
		}
		
		public RelationWriter() {
			super();
		}
			
		public IRelation done() {
			if(constructedSet == null){
				constructedSet = new Relation(setContent.isEmpty() ? TypeFactory.getInstance().voidType() : eltType, setContent);
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
	
	public ISet selectByFieldNames(String... fields) {
		int[] indexes = new int[fields.length];
		int i = 0;
		
		if (getFieldTypes().hasFieldNames()) {
			for (String field : fields) {
				indexes[i++] = getFieldTypes().getFieldIndex(field);
			}
			
			return select(indexes);
		}
		
		throw new IllegalOperationException("select with field names", getType());
	}
}