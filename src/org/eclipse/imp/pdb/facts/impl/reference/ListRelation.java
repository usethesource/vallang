/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* 	Paul Klint (Paul.Klint@cwi.nl) - added new ListRelation datatype
* based on code by
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.HashSet;
import java.util.LinkedList;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListRelation;
import org.eclipse.imp.pdb.facts.IListRelationWriter;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.impl.fast.Tuple;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

public class ListRelation extends List implements IListRelation {
	protected final Type listRelationType;

	/* package */ ListRelation(Type type, LinkedList<IValue> content) {
		super(type, content);
		this.listRelationType =TypeFactory.getInstance().lrelTypeFromTuple(type);
	}
	
	public int arity() {
		return getType().getArity();
	}
	
	public IListRelation closure() throws FactTypeUseException {
		Type resultType = getType().closure(); // will throw exception if not binary and reflexive
		IListRelation tmp = this;

		int prevCount = 0;

		while (prevCount != tmp.length()) {
			prevCount = tmp.length();
			IListRelation tcomp = tmp.compose(tmp);
			IListRelationWriter w = ValueFactory.getInstance().listRelationWriter(resultType.getElementType());
			for(IValue t1 : tcomp){
				if(!tmp.contains(t1))
					w.append(t1);
			}
			tmp = tmp.concat(w.done());
		}

		return tmp;
	}
	
	public IListRelation closureStar() throws FactTypeUseException {
		Type resultType = getType().closure();
		// an exception will have been thrown if the type is not acceptable

		IListRelationWriter reflex = ListRelation.createRelationWriter(resultType.getElementType());

		for (IValue e: carrier()) {
			reflex.insert(new Tuple(new IValue[] {e, e}));
		}
		
		return (IListRelation) closure().concat(reflex.done());
	}

	public IListRelation compose(IRelation other) throws FactTypeUseException {
		Type resultType = getType().compose(other.getType());
		// an exception will have been thrown if the relations are not both binary and
		// have a comparable field to compose.
		IListRelationWriter w = ValueFactory.getInstance().listRelationWriter(resultType.getFieldTypes());

		for (IValue v1 : content) {
			ITuple tuple1 = (ITuple) v1;
			for (IValue t2 : other) {
				ITuple tuple2 = (ITuple) t2;
				
				if (tuple1.get(1).isEqual(tuple2.get(0))) {
					ITuple tup = new Tuple(tuple1.get(0), tuple2.get(1));
					if(!content.contains(tup))
						w.append(tup);
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
		return fType.getFieldTypes();
	}
	
	public static IListRelationWriter createRelationWriter(Type tupleType) {
		return new ListRelationWriter(tupleType);
	}
	
	public static IListRelationWriter createRelationWriter() {
		return new ListRelationWriter();
	}
	
	protected static class ListRelationWriter  extends List.ListWriter implements IListRelationWriter {
		public ListRelationWriter(Type eltType) {
			super(eltType);
		}
		
		public ListRelationWriter() {
			super();
		}
			
		public IListRelation done() {
			if(constructedList == null){
				constructedList = new ListRelation(listContent.isEmpty() ? TypeFactory.getInstance().voidType() : eltType, listContent);
			}
			return  (IListRelation) constructedList;
		}

		public int size() {
			// TODO Auto-generated method stub
			return 0;
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
	
	public IListRelation compose(IListRelation other) throws FactTypeUseException {
		Type resultType = getType().compose(other.getType());
		// an exception will have been thrown if the relations are not both binary and
		// have a comparable field to compose.
		IListRelationWriter w = ValueFactory.getInstance().listRelationWriter(resultType.getFieldTypes());

		for (IValue v1 : content) {
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

	public static IListRelationWriter createListRelationWriter() {
		return  new ListRelationWriter();
	}
	
	public static IListRelationWriter createListRelationWriter(Type tupleType) {
		return new ListRelationWriter(tupleType);
	}
}