/*******************************************************************************
* Copyright (c) 2012 Centrum Wiskunde & Informatica
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	 Paul Klint (Paul.Klint@cwi.nl)         - Added ListRelation datatype
* Based on code by   
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.fast;

import java.util.HashSet;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListRelation;
import org.eclipse.imp.pdb.facts.IListRelationWriter;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashSet;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class ListRelation extends List implements IListRelation {
	
	/*package*/ ListRelation(Type type, ShareableValuesList content) {
		super(type, content);
		assert listType == typeFactory.lrelTypeFromTuple(type);
	}
	
	public int arity() {
		return this.elementType.getArity();
	}

	public IListRelation closure() throws FactTypeUseException {
		Type resultType = getType().closure(); // will throw exception if not binary and reflexive
		IListRelation tmp = this;

		int prevCount = 0;

		ShareableValuesHashSet addedTuples = new ShareableValuesHashSet();
		while (prevCount != tmp.length()) {
			prevCount = tmp.length();
			IListRelation tcomp = tmp.compose(tmp);
			IListRelationWriter w = ListRelation.createListRelationWriter(resultType.getElementType());
			for(IValue t1 : tcomp){
				if(!tmp.contains(t1)){
					if(!addedTuples.contains(t1)){
						addedTuples.add(t1);
						w.append(t1);
					}
				}
			}
			tmp = tmp.concat(w.done());
			addedTuples.clear();
		}
		return tmp;
	}
	
	public IListRelation closureStar() throws FactTypeUseException {
		Type resultType = getType().closure();
		// an exception will have been thrown if the type is not acceptable

		IListRelationWriter reflex = ListRelation.createListRelationWriter(resultType.getElementType());

		for (IValue e: carrier()) {
			reflex.insert(new Tuple(new IValue[] {e, e}));
		}
		
		return (IListRelation) closure().concat(reflex.done());
	}

	public IListRelation compose(IListRelation other) throws FactTypeUseException {
		
		Type otherTupleType = other.getFieldTypes();
		
		if(elementType == voidType) return this;
		if(otherTupleType == voidType) return other;
		
		if(elementType.getArity() != 2 || otherTupleType.getArity() != 2) throw new IllegalOperationException("compose", elementType, otherTupleType);
		
		// Relaxed type constraint:
	    if(!elementType.getFieldType(1).comparable(otherTupleType.getFieldType(0))) throw new IllegalOperationException("compose", elementType, otherTupleType);

		Type[] newTupleFieldTypes = new Type[]{elementType.getFieldType(0), otherTupleType.getFieldType(1)};
		Type tupleType = typeFactory.tupleType(newTupleFieldTypes);

		IListRelationWriter w = ValueFactory.getInstance().listRelationWriter(tupleType);

		for (IValue v1 : data) {
			ITuple tuple1 = (ITuple) v1;
			for (IValue t2 : other) {
				ITuple tuple2 = (ITuple) t2;
				
				if (tuple1.get(1).isEqual(tuple2.get(0))) {
						w.append(new Tuple(tuple1.get(0), tuple2.get(1)));
				}
			}
		}
		return w.done();
	}

	public IList carrier() {
		Type newType = getType().carrier();
		IListWriter w = List.createListWriter(newType.getElementType());
		HashSet<IValue> cache = new HashSet<IValue>();
		
		for (IValue v : this) {
			ITuple t = (ITuple) v;
			for(IValue e : t){
				if(!cache.contains(e)){
					cache.add(e);
					w.append(e);
				}
			}
		}
		
		return w.done();
	}

	public IList domain() {
		Type lrelType = getType();
		IListWriter w = List.createListWriter(lrelType.getFieldType(0));
		HashSet<IValue> cache = new HashSet<IValue>();
		
		for (IValue elem : this) {
			ITuple tuple = (ITuple) elem;
			IValue e = tuple.get(0);
			if(!cache.contains(e)){
				cache.add(e);
				w.append(e);
			}
		}
		return w.done();
	}
	
	public IList range() {
		Type lrelType = getType();
		int last = lrelType.getArity() - 1;
		IListWriter w = List.createListWriter(lrelType.getFieldType(last));
		HashSet<IValue> cache = new HashSet<IValue>();
		
		for (IValue elem : this) {
			ITuple tuple = (ITuple) elem;
			IValue e = tuple.get(last);
			if(!cache.contains(e)){
				cache.add(e);
				w.append(e);
			}
		}
		
		return w.done();
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitListRelation(this);
	}
	
	public Type getFieldTypes() {
		return elementType; //listRelationType.getFieldTypes();
	}
	
	public static IListRelationWriter createListRelationWriter(Type tupleType) {
		return new ListRelationWriter(tupleType);
	}
	
	public static IListRelationWriter createListRelationWriter() {
		return new ListRelationWriter();
	}
	
	protected static class ListRelationWriter  extends ListWriter implements IListRelationWriter {
		public ListRelationWriter(Type eltType) {
			super(eltType);
		}
		
		public ListRelationWriter() {
			super();
		}
			
		public IListRelation done() {
			if(constructedList == null){
				constructedList = new ListRelation(data.isEmpty() ? TypeFactory.getInstance().voidType() : elementType, data);
			}
			return  (IListRelation) constructedList;
		}

		public int size() {
			return super.size();
		}
	}
	
	public IList select(int... fields) {
		Type eltType = getFieldTypes().select(fields);
		IListWriter w = ValueFactory.getInstance().listWriter(eltType);
		
		for (IValue v : this) {
			w.append(((ITuple) v).select(fields));
		}
		
		return w.done();
	}
	
	public IList selectByFieldNames(String... fields) {
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