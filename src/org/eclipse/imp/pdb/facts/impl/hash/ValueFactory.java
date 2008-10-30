/*******************************************************************************
* Copyright (c) 2007, 2008 IBM Corporation & CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.hash;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITree;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.BaseValueFactory;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.RelationType;
import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public class ValueFactory extends BaseValueFactory {
	private static final ValueFactory sInstance = new ValueFactory();

	public static ValueFactory getInstance() {
		return sInstance;
	}

	private ValueFactory() {
	}

	public IRelation relation(NamedType relationType) throws FactTypeError {
		return new Relation(relationType);
	}

	public IRelation relation(TupleType tupleType) {
		return new Relation(TypeFactory.getInstance().relType(tupleType));
	}

	public IRelation relationWith(ITuple t) {
		Relation result = new Relation((TupleType) t.getBaseType());
		IRelationWriter rw = result.getWriter();
		try {
			rw.insert(t);
		} catch (FactTypeError e) {
			// this never happens
		}
		return result;
	}

	public IRelation relationWith(ITuple t1, ITuple t2) throws FactTypeError {
		Type eltType = t1.getType().lub(t2.getType());

		if (!eltType.getBaseType().isTupleType()) {
			throw new FactTypeError("Arities of tuples are not compatible");
		}

		RelationType relType = TypeFactory.getInstance().relType(
				(TupleType) eltType.getBaseType());
		Relation result = new Relation(relType);
		IRelationWriter rw = result.getWriter();
		rw.insert(t1);
		rw.insert(t2);
		return result;
	}

	public IRelation relationWith(ITuple t1, ITuple t2, ITuple t3)
			throws FactTypeError {
		Type eltType = t1.getType().lub(t2.getType()).lub(t3.getType());

		if (!eltType.getBaseType().isTupleType()) {
			throw new FactTypeError("Arities of tuples are not compatible");
		}

		RelationType relType = TypeFactory.getInstance().relType(
				(TupleType) eltType.getBaseType());
		Relation result = new Relation(relType);
		IRelationWriter rw = result.getWriter();
		rw.insert(t1);
		rw.insert(t2);
		rw.insert(t3);
		return result;
	}

	public IRelation relationWith(ITuple t1, ITuple t2, ITuple t3, ITuple t4)
			throws FactTypeError {
		Type eltType = (TupleType) t1.getType().lub(t2.getType()).lub(
				t3.getType()).lub(t4.getType());

		if (!eltType.getBaseType().isTupleType()) {
			throw new FactTypeError("Arities of tuples are not compatible");
		}

		RelationType relType = TypeFactory.getInstance().relType(
				(TupleType) eltType.getBaseType());
		Relation result = new Relation(relType);
		IRelationWriter rw = result.getWriter();
		rw.insert(t1);
		rw.insert(t2);
		rw.insert(t3);
		rw.insert(t4);
		return result;
	}

	public IRelation relationWith(ITuple t1, ITuple t2, ITuple t3, ITuple t4,
			ITuple t5) throws FactTypeError {
		Type eltType = t1.getType().lub(t2.getType()).lub(t3.getType()).lub(
				t4.getType()).lub(t5.getType());

		if (!eltType.getBaseType().isTupleType()) {
			throw new FactTypeError("Arities of tuples are not compatible");
		}

		RelationType relType = TypeFactory.getInstance().relType(
				(TupleType) eltType.getBaseType());
		Relation result = new Relation(relType);
		IRelationWriter rw = result.getWriter();
		rw.insert(t1);
		rw.insert(t2);
		rw.insert(t3);
		rw.insert(t4);
		rw.insert(t5);
		return result;
	}

	public IRelation relationWith(ITuple t1, ITuple t2, ITuple t3, ITuple t4,
			ITuple t5, ITuple t6) throws FactTypeError {
		Type eltType = t1.getType().lub(t2.getType()).lub(t3.getType()).lub(
				t4.getType()).lub(t5.getType()).lub(t6.getType());

		if (!eltType.getBaseType().isTupleType()) {
			throw new FactTypeError("Arities of tuples are not compatible");
		}

		RelationType relType = TypeFactory.getInstance().relType(
				(TupleType) eltType.getBaseType());
		Relation result = new Relation(relType);
		IRelationWriter rw = result.getWriter();
		rw.insert(t1);
		rw.insert(t2);
		rw.insert(t3);
		rw.insert(t4);
		rw.insert(t5);
		rw.insert(t6);
		return result;
	}

	public IRelation relationWith(ITuple t1, ITuple t2, ITuple t3, ITuple t4,
			ITuple t5, ITuple t6, ITuple t7) throws FactTypeError {
		Type eltType = (TupleType) t1.getType().lub(t2.getType()).lub(
				t3.getType()).lub(t4.getType()).lub(t5.getType()).lub(
				t6.getType()).lub(t7.getType());

		if (!eltType.getBaseType().isTupleType()) {
			throw new FactTypeError("Arities of tuples are not compatible");
		}

		RelationType relType = TypeFactory.getInstance().relType(
				(TupleType) eltType);
		Relation result = new Relation(relType);
		IRelationWriter rw = result.getWriter();
		rw.insert(t1);
		rw.insert(t2);
		rw.insert(t3);
		rw.insert(t4);
		rw.insert(t5);
		rw.insert(t6);
		rw.insert(t7);
		return result;
	}

	public ISet set(NamedType setType) throws FactTypeError {
		return new Set(setType);
	}

	public ISet set(Type eltType) {
		return new Set(eltType);
	}

	public ISet setWith(IValue t) {
		Set result = new Set(t.getType());
		ISetWriter sw = result.getWriter();
		try {
			sw.insert(t);
		} catch (FactTypeError e) {
			// this never happens
		}
		return result;
	}

	public ISet setWith(IValue t1, IValue t2) {
		Type eltType = t1.getType().lub(t2.getType());
		Set result = new Set(eltType);
		ISetWriter sw = result.getWriter();
		try {
			sw.insert(t1);
			sw.insert(t2);
		} catch (FactTypeError e) {
			// this never happens
		}

		return result;
	}

	// Bogus - shouldn't create a relType if the IValue's aren't tuples...
	public ISet setWith(IValue t1, IValue t2, IValue t3) {
		Type eltType = t1.getType().lub(t2.getType()).lub(t3.getType());
		Set result = new Set(eltType);
		ISetWriter sw = result.getWriter();
		try {
			sw.insert(t1);
			sw.insert(t2);
			sw.insert(t3);
		} catch (FactTypeError e) {
			// this never happens
		}
		return result;
	}

	public ISet setWith(IValue t1, IValue t2, IValue t3, IValue t4) {
		Type eltType = t1.getType().lub(t2.getType()).lub(t3.getType()).lub(
				t4.getType());
		Set result = new Set(eltType);
		ISetWriter sw = result.getWriter();
		try {
			sw.insert(t1);
			sw.insert(t2);
			sw.insert(t3);
			sw.insert(t4);
		} catch (FactTypeError e) {
			// this never happens
		}
		return result;
	}

	public ISet setWith(IValue t1, IValue t2, IValue t3, IValue t4, IValue t5) {
		Type eltType = t1.getType().lub(t2.getType()).lub(t3.getType()).lub(
				t4.getType()).lub(t5.getType());
		Set result = new Set(eltType);
		ISetWriter sw = result.getWriter();
		try {
			sw.insert(t1);
			sw.insert(t2);
			sw.insert(t3);
			sw.insert(t4);
			sw.insert(t5);
		} catch (FactTypeError e) {
			// this never happens
		}
		return result;
	}

	public ISet setWith(IValue t1, IValue t2, IValue t3, IValue t4, IValue t5,
			IValue t6) {
		Type eltType = t1.getType().lub(t2.getType()).lub(t3.getType()).lub(
				t4.getType()).lub(t5.getType()).lub(t6.getType());
		Set result = new Set(eltType);
		ISetWriter sw = result.getWriter();
		try {
			sw.insert(t1);
			sw.insert(t2);
			sw.insert(t3);
			sw.insert(t4);
			sw.insert(t5);
			sw.insert(t6);
		} catch (FactTypeError e) {
			// this never happens
		}
		return result;
	}

	public ISet setWith(IValue t1, IValue t2, IValue t3, IValue t4, IValue t5,
			IValue t6, IValue t7) {
		Type eltType = t1.getType().lub(t2.getType()).lub(t3.getType()).lub(
				t4.getType()).lub(t5.getType()).lub(t6.getType()).lub(
				t7.getType());
		Set result = new Set(eltType);
		ISetWriter sw = result.getWriter();
		try {
			sw.insert(t1);
			sw.insert(t2);
			sw.insert(t3);
			sw.insert(t4);
			sw.insert(t5);
			sw.insert(t6);
			sw.insert(t7);
		} catch (FactTypeError e) {
			// this never happens
		}
		return result;
	}

	public IList list(NamedType listType) throws FactTypeError {
		return new List(listType);
	}

	public IList list(Type eltType) {
		return new List(eltType);
	}

	/**
	 * Creates an IList with the given value as an element. Upon return, the
	 * IList is in the "open" state, so you can retrieve the IListWriter and
	 * continue adding elements. Remember to call IListWriter.done() to "close"
	 * the list and mark it immutable.
	 */
	public IList listWith(IValue a) {
		List result = new List(a.getType());
		IListWriter lw = result.getWriter();
			lw.insert(a);
		return result;
	}

	/**
	 * Creates an IList with the given values as elements. Upon return, the
	 * IList is in the "open" state, so you can retrieve the IListWriter and
	 * continue adding elements. Remember to call IListWriter.done() to "close"
	 * the list and mark it immutable.
	 */
	public IList listWith(IValue a, IValue b) {
		List result = new List(a.getType());
		IListWriter lw = result.getWriter();
			lw.insert(b);
			lw.insert(a);

		return result;
	}

	/**
	 * Creates an IList with the given values as elements. Upon return, the
	 * IList is in the "open" state, so you can retrieve the IListWriter and
	 * continue adding elements. Remember to call IListWriter.done() to "close"
	 * the list and mark it immutable.
	 */
	public IList listWith(IValue a, IValue b, IValue c) {
		List result = new List(a.getType());
		IListWriter lw = result.getWriter();
			lw.insert(c);
			lw.insert(b);
			lw.insert(a);

		return result;
	}

	/**
	 * Creates an IList with the given values as elements. Upon return, the
	 * IList is in the "open" state, so you can retrieve the IListWriter and
	 * continue adding elements. Remember to call IListWriter.done() to "close"
	 * the list and mark it immutable.
	 */
	public IList listWith(IValue a, IValue b, IValue c, IValue d) {
		List result = new List(a.getType());
		IListWriter lw = result.getWriter();
			lw.insert(d);
			lw.insert(c);
			lw.insert(b);
			lw.insert(a);

		return result;
	}

	/**
	 * Creates an IList with the given values as elements. Upon return, the
	 * IList is in the "open" state, so you can retrieve the IListWriter and
	 * continue adding elements. Remember to call IListWriter.done() to "close"
	 * the list and mark it immutable.
	 */
	public IList listWith(IValue a, IValue b, IValue c, IValue d, IValue e) {
		List result = new List(a.getType());
		IListWriter lw = result.getWriter();
			lw.insert(e);
			lw.insert(d);
			lw.insert(c);
			lw.insert(b);
			lw.insert(a);

		return result;
	}

	/**
	 * Creates an IList with the given values as elements. Upon return, the
	 * IList is in the "open" state, so you can retrieve the IListWriter and
	 * continue adding elements. Remember to call IListWriter.done() to "close"
	 * the list and mark it immutable.
	 */
	public IList listWith(IValue a, IValue b, IValue c, IValue d, IValue e,
			IValue f) {
		List result = new List(a.getType());
		IListWriter lw = result.getWriter();
			lw.insert(f);
			lw.insert(e);
			lw.insert(d);
			lw.insert(c);
			lw.insert(b);
			lw.insert(a);

		return result;
	}

	/**
	 * Creates an IList with the given values as elements. Upon return, the
	 * IList is in the "open" state, so you can retrieve the IListWriter and
	 * continue adding elements. Remember to call IListWriter.done() to "close"
	 * the list and mark it immutable.
	 */
	public IList listWith(IValue a, IValue b, IValue c, IValue d, IValue e,
			IValue f, IValue g) {
		List result = new List(a.getType());
		IListWriter lw = result.getWriter();
			lw.insert(g);
			lw.insert(f);
			lw.insert(e);
			lw.insert(d);
			lw.insert(c);
			lw.insert(b);
			lw.insert(a);

		return result;
	}

	public ITuple tuple(IValue a) {
		return new Tuple(a);
	}

	public ITuple tuple(IValue a, IValue b) {
		return new Tuple(a, b);
	}

	public ITuple tuple(IValue a, IValue b, IValue c) {
		return new Tuple(a, b, c);
	}

	public ITuple tuple(IValue a, IValue b, IValue c, IValue d) {
		return new Tuple(a, b, c, d);
	}

	public ITuple tuple(IValue a, IValue b, IValue c, IValue d, IValue e) {
		return new Tuple(a, b, c, d, e);
	}

	public ITuple tuple(IValue a, IValue b, IValue c, IValue d, IValue e,
			IValue f) {
		return new Tuple(a, b, c, d, e, f);
	}

	public ITuple tuple(IValue a, IValue b, IValue c, IValue d, IValue e,
			IValue f, IValue g) {
		return new Tuple(a, b, c, d, e, f, g);
	}
	
	public ITuple tuple(IValue[] elements, int size) {
		IValue[] tmp = new IValue[size];
		System.arraycopy(elements, 0, tmp, 0, size);
		return new Tuple(tmp);
	}
	
	public ITuple tuple(NamedType type, IValue[] elements, int size) {
		IValue[] tmp = new IValue[size];
		System.arraycopy(elements, 0, tmp, 0, size);
		return new Tuple(type, tmp);
	}
	
	public ITree tree(TreeNodeType type, IValue[] children) {
		return new Tree(this, type, children);
	}
	
	public ITree tree(NamedType type, IValue[] children) {
		if (!type.getBaseType().isTreeNodeType()) {
			throw new FactTypeError(type + " is not a tree type");
		}
		return new Tree(this, type, children);
	}

	public ITree tree(TreeNodeType type, java.util.List<IValue> children) {
		if (!type.getBaseType().isTreeNodeType()) {
			throw new FactTypeError(type + " is not a tree type");
		}
		return new Tree(this, type, children);
	}

	
	public ITree tree(TreeNodeType type) {
		return new Tree(this, type);
	}

	public ITree tree(TreeNodeType type, IValue child1) {
		return new Tree(this, type, new IValue[] { child1 });
	}

	public ITree tree(TreeNodeType type, IValue child1, IValue child2) {
		return new Tree(this, type, new IValue[] { child1, child2 });
	}

	public ITree tree(TreeNodeType type, IValue child1, IValue child2,
			IValue child3) {
		return new Tree(this, type, new IValue[] { child1, child2, child3 });
	}

	public ITree tree(TreeNodeType type, IValue child1, IValue child2,
			IValue child3, IValue child4) {
		return new Tree(this, type, new IValue[] { child1, child2, child3, child4 });
	}

	public ITree tree(TreeNodeType type, IValue child1, IValue child2,
			IValue child3, IValue child4, IValue child5) {
		return new Tree(this, type, new IValue[] { child1, child2, child3, child4, child5 });
	}

	public ITree tree(TreeNodeType type, IValue child1, IValue child2,
			IValue child3, IValue child4, IValue child5, IValue child6) {
		return new Tree(this, type, new IValue[] { child1, child2, child3, child4, child5, child6 });
	}

	public ITree tree(TreeNodeType type, IValue child1, IValue child2,
			IValue child3, IValue child4, IValue child5, IValue child6,
			IValue child7) {
		return new Tree(this, type, new IValue[] { child1, child2, child3, child4, child5, child6, child7 });
	}

	public ITree tree(NamedType type) {
		return new Tree(this, type);
	}

	public ITree tree(NamedType type, IValue child1) {
		return new Tree(this, type, new IValue[] { child1 });
	}

	public ITree tree(NamedType type, IValue child1, IValue child2) {
		return new Tree(this, type, new IValue[] { child1, child2 });
	}

	public ITree tree(NamedType type, IValue child1, IValue child2,
			IValue child3) {
		return new Tree(this, type, new IValue[] { child1, child2, child3 });
	}

	public ITree tree(NamedType type, IValue child1, IValue child2,
			IValue child3, IValue child4) {
		return new Tree(this, type, new IValue[] { child1, child2, child3, child4 });
	}

	public ITree tree(NamedType type, IValue child1, IValue child2,
			IValue child3, IValue child4, IValue child5) {
		return new Tree(this, type, new IValue[] { child1, child2, child3, child4, child5 });
	}

	public ITree tree(NamedType type, IValue child1, IValue child2,
			IValue child3, IValue child4, IValue child5, IValue child6) {
		return new Tree(this, type, new IValue[] { child1, child2, child3, child4, child5, child6 });
	}

	public ITree tree(NamedType type, IValue child1, IValue child2,
			IValue child3, IValue child4, IValue child5, IValue child6,
			IValue child7) {
		return new Tree(this, type, new IValue[] { child1, child2, child3, child4, child5, child6, child7 });
	}

	public IMap map(Type key, Type value) {
		return new Map(key, value);
	}

	public IMap map(NamedType type) {
		return new Map(type);
	}
}
