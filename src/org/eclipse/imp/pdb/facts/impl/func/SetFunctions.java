/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse public static License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *
 * Based on code by:
 *
 *   * Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.func;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public final class SetFunctions {

	private final static TypeFactory TF = TypeFactory.getInstance();

	public static boolean contains(IValueFactory vf, ISet set1, IValue e) {
		for (IValue v : set1) {
			if (v.isEqual(e)) {
				return true;
			}
		}
		return false;
	}

	public static ISet insert(IValueFactory vf, ISet set1, IValue e) {
		ISetWriter sw = vf.setWriter();
		sw.insertAll(set1);
		sw.insert(e);
		return sw.done();
	}

	public static ISet intersect(IValueFactory vf, ISet set1, ISet set2) {
		ISetWriter w = vf.setWriter();

		for (IValue v : set1) {
			if (set2.contains(v)) {
				w.insert(v);
			}
		}

		return w.done();
	}

	public static ISet subtract(IValueFactory vf, ISet set1, ISet set2) {
		ISetWriter sw = vf.setWriter();
		for (IValue a : set1) {
			if (!set2.contains(a)) {
				sw.insert(a);
			}
		}
		return sw.done();
	}

	public static ISet delete(IValueFactory vf, ISet set1, IValue v) {
		ISetWriter w = vf.setWriter();

		boolean deleted = false;
		for (Iterator<IValue> iterator = set1.iterator(); iterator.hasNext();) {
			IValue e = iterator.next();

			if (!deleted && e.isEqual(v)) {
				deleted = true; // skip first occurrence
			} else {
				w.insert(e);
			}
		}
		return w.done();
	}

	public static boolean isSubsetOf(IValueFactory vf, ISet set1, ISet set2) {
		for (IValue elem : set1) {
			if (!set2.contains(elem)) {
				return false;
			}
		}
		return true;
	}

	public static ISet union(IValueFactory vf, ISet set1, ISet set2) {
		ISetWriter w = vf.setWriter();
		w.insertAll(set1);
		w.insertAll(set2);
		return w.done();
	}

	public static int hashCode(IValueFactory vf, ISet set1) {
		int hash = 0;

		Iterator<IValue> iterator = set1.iterator();
		while (iterator.hasNext()) {
			IValue element = iterator.next();
			hash ^= element.hashCode();
		}

		return hash;
	}

	public static boolean equals(IValueFactory vf, ISet set1, Object other) {
		if (other == set1)
			return true;
		if (other == null)
			return false;

		if (other instanceof ISet) {
			ISet set2 = (ISet) other;

			if (set1.getType() != set2.getType())
				return false;

			if (hashCode(vf, set1) != hashCode(vf, set2))
				return false;

			if (set1.size() == set2.size()) {

				for (IValue v1 : set1) {
					if (set2.contains(v1) == false)
						return false;
				}

				return true;
			}
		}

		return false;
	}

	/*
	 * NOTE: it's actually difficult to support isEqual semantics for sets if
	 * it is not supported by the underlying container.
	 */
	public static boolean isEqual(IValueFactory vf, ISet set1, IValue other) {
//		return equals(vf, set1, other);		
		if (other == set1)
			return true;
		if (other == null)
			return false;
			
		if (other instanceof ISet) {
			ISet set2 = (ISet) other;

			if (set1.size() == set2.size()) {

				for (IValue v1 : set1) {
					// function contains() calls isEqual() but used O(n) time
					if (contains(vf, set2, v1) == false)
						return false;
				}

				return true;
			}
		}

		return false;
	}

	public static ISet product(IValueFactory vf, ISet set1, ISet set2) {
		ISetWriter w = vf.setWriter();

		for (IValue t1 : set1) {
			for (IValue t2 : set2) {
				ITuple t3 = vf.tuple(t1, t2);
				w.insert(t3);
			}
		}

		return w.done();
	}

	public static ISet closure(IValueFactory vf, ISet set1)
			throws FactTypeUseException {
		// will throw exception if not binary and reflexive
		set1.getType().closure();

		ISet tmp = set1;

		int prevCount = 0;

		while (prevCount != tmp.size()) {
			prevCount = tmp.size();
			tmp = tmp.union(compose(vf, tmp, tmp));
		}

		return tmp;
	}

	public static ISet closureStar(IValueFactory vf, ISet set1)
			throws FactTypeUseException {
		set1.getType().closure();
		// an exception will have been thrown if the type is not acceptable

		ISetWriter reflex = vf.setWriter();

		for (IValue e : carrier(vf, set1)) {
			reflex.insert(vf.tuple(new IValue[] { e, e }));
		}

		return closure(vf, set1).union(reflex.done());
	}

	public static ISet compose(IValueFactory vf, ISet set1, ISet set2)
			throws FactTypeUseException {
		if (set1.getElementType() == TF.voidType())
			return set1;
		if (set2.getElementType() == TF.voidType())
			return set2;

		if (set1.getElementType().getArity() != 2
				|| set2.getElementType().getArity() != 2) {
			throw new IllegalOperationException(
					"Incompatible types for composition.",
					set1.getElementType(), set2.getElementType());
		}

		ISetWriter w = vf.setWriter();

		if (set1.getElementType().getFieldType(1)
				.comparable(set2.getElementType().getFieldType(0))) {
			for (IValue v1 : set1) {
				ITuple tuple1 = (ITuple) v1;
				for (IValue t2 : set2) {
					ITuple tuple2 = (ITuple) t2;

					if (tuple1.get(1).isEqual(tuple2.get(0))) {
						w.insert(vf.tuple(tuple1.get(0), tuple2.get(1)));
					}
				}
			}
		}
		return w.done();
	}

	public static ISet carrier(IValueFactory vf, ISet set1) {
		ISetWriter w = vf.setWriter();

		for (IValue t : set1) {
			w.insertAll((ITuple) t);
		}

		return w.done();
	}

	public static ISet domain(IValueFactory vf, ISet set1) {
		int columnIndex = 0;
		ISetWriter w = vf.setWriter();

		for (IValue elem : set1) {
			ITuple tuple = (ITuple) elem;
			w.insert(tuple.get(columnIndex));
		}
		return w.done();
	}

	public static ISet range(IValueFactory vf, ISet set1) {
		int columnIndex = set1.getType().getArity() - 1;
		ISetWriter w = vf.setWriter();

		for (IValue elem : set1) {
			ITuple tuple = (ITuple) elem;
			w.insert(tuple.get(columnIndex));
		}

		return w.done();
	}

	public static ISet project(IValueFactory vf, ISet set1, int... fields) {
		ISetWriter w = vf.setWriter();

		for (IValue v : set1) {
			w.insert(((ITuple) v).select(fields));
		}

		return w.done();
	}

	public static ISet projectByFieldNames(IValueFactory vf, ISet set1,
			String... fields) {
		int[] indexes = new int[fields.length];
		int i = 0;

		if (set1.getType().getFieldTypes().hasFieldNames()) {
			for (String field : fields) {
				indexes[i++] = set1.getType().getFieldTypes()
						.getFieldIndex(field);
			}

			return project(vf, set1, indexes);
		}

		throw new IllegalOperationException("select with field names",
				set1.getType());
	}

}
