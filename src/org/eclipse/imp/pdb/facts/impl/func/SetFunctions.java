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

import org.eclipse.imp.pdb.facts.*;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public final class SetFunctions {

    @SuppressWarnings("unused")
	private final static TypeFactory TF = TypeFactory.getInstance();

//    public static boolean contains(IValueFactory vf, ISet set1, IValue e) {
//        for (IValue v : set1) {
//            if (v.equals(e)) {
//                return true;
//            }
//        }
//        return false;
//    }

    public static ISet insert(IValueFactory vf, ISet set1, IValue e) {
        ISetWriter sw = vf.setWriter(set1.getElementType().lub(e.getType()));
        sw.insertAll(set1);
        sw.insert(e);
        return sw.done();
    }

    public static ISet intersect(IValueFactory vf, ISet set1, ISet set2) {
        ISetWriter w = vf.setWriter(set2.getElementType().lub(set1.getElementType()));

        for (IValue v : set1) {
            if (set2.contains(v)) {
                w.insert(v);
            }
        }

        return w.done();
    }

    public static ISet subtract(IValueFactory vf, ISet set1, ISet set2) {
        ISetWriter sw = vf.setWriter(set1.getElementType());
        for (IValue a : set1) {
            if (!set2.contains(a)) {
                sw.insert(a);
            }
        }
        return sw.done();
    }

    public static ISet delete(IValueFactory vf, ISet set1, IValue e) {
        ISetWriter sw = vf.setWriter(set1.getElementType());
        sw.insertAll(set1);
        sw.delete(e);
        return sw.done();
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
        ISetWriter w = vf.setWriter(set2.getElementType().lub(set1.getElementType()));
        w.insertAll(set1);
        w.insertAll(set2);
        return w.done();
    }

    public static boolean isEqual(IValueFactory vf, ISet set1, IValue other) {
        return equals(vf, set1, other);
    }

    public static boolean equals(IValueFactory vf, ISet set1, Object other) {
        if (other == set1) return true;
        if (other == null) return false;

        if (other instanceof ISet) {
            ISet set2 = (ISet) other;

            if (set1.getType() != set2.getType()) return false;

            if (set1.hashCode() != set2.hashCode()) return false;

            if (set1.size() == set2.size()) {

                for (IValue v1 : set1) {
                    if (set2.contains(v1) == false) return false;
                }

                return true;
            }
        }

        return false;
    }

    public static IRelation product(IValueFactory vf, ISet set1, ISet set2) {
        Type resultType = TypeFactory.getInstance().tupleType(set1.getElementType(), set2.getElementType());
        IRelationWriter w = vf.relationWriter(resultType);

        for (IValue t1 : set1) {
            for (IValue t2 : set2) {
                ITuple t3 = vf.tuple(t1, t2);
                w.insert(t3);
            }
        }

        return w.done();
    }

    public static ISet closure(IValueFactory vf, ISet set1) throws FactTypeUseException {
        set1.getType().closure(); // will throw exception if not binary and reflexive
        ISet tmp = set1;

        int prevCount = 0;

        while (prevCount != tmp.size()) {
            prevCount = tmp.size();
            tmp = tmp.union(compose(vf, tmp, tmp));
        }

        return tmp;
    }

    public static IRelation closureStar(IValueFactory vf, ISet set1) throws FactTypeUseException {
        Type resultType = set1.getType().closure();
        // an exception will have been thrown if the type is not acceptable

        ISetWriter reflex = vf.relationWriter(resultType.getElementType());

        for (IValue e : carrier(vf, set1)) {
            reflex.insert(vf.tuple(new IValue[]{e, e}));
        }

        return closure(vf, set1).union(reflex.done());
    }

    public static ISet compose(IValueFactory vf, ISet set1, ISet set2) throws FactTypeUseException {
        Type resultType = set1.getType().compose(set2.getType());
        // an exception will have been thrown if the relations are not both binary and
        // have a comparable field to compose.
        ISetWriter w = vf.relationWriter(resultType.getFieldTypes());

        for (IValue v1 : set1) {
            ITuple tuple1 = (ITuple) v1;
            for (IValue t2 : set2) {
                ITuple tuple2 = (ITuple) t2;

                if (tuple1.get(1).equals(tuple2.get(0))) {
                    w.insert(vf.tuple(tuple1.get(0), tuple2.get(1)));
                }
            }
        }
        return w.done();
    }

    public static ISet carrier(IValueFactory vf, ISet set1) {
        Type newType = set1.getType().carrier();
        ISetWriter w = vf.setWriter(newType.getElementType());

        for (IValue t : set1) {
            w.insertAll((ITuple) t);
        }

        return w.done();
    }

    public static ISet domain(IValueFactory vf, ISet set1) {
        Type relType = set1.getType();
        ISetWriter w = vf.setWriter(relType.getFieldType(0));

        for (IValue elem : set1) {
            ITuple tuple = (ITuple) elem;
            w.insert(tuple.get(0));
        }
        return w.done();
    }

    public static ISet range(IValueFactory vf, ISet set1) {
        Type relType = set1.getType();
        int last = relType.getArity() - 1;
        ISetWriter w = vf.setWriter(relType.getFieldType(last));

        for (IValue elem : set1) {
            ITuple tuple = (ITuple) elem;
            w.insert(tuple.get(last));
        }

        return w.done();
    }

    public static ISet project(IValueFactory vf, ISet set1, int... fields) {
        Type eltType = set1.getType().getFieldTypes().select(fields);
        ISetWriter w = vf.setWriter(eltType);

        for (IValue v : set1) {
            w.insert(((ITuple) v).select(fields));
        }

        return w.done();
    }

    public static ISet projectByFieldNames(IValueFactory vf, ISet set1, String... fields) {
        int[] indexes = new int[fields.length];
        int i = 0;

        if (set1.getType().getFieldTypes().hasFieldNames()) {
            for (String field : fields) {
                indexes[i++] = set1.getType().getFieldTypes().getFieldIndex(field);
            }

            return project(vf, set1, indexes);
        }

        throw new IllegalOperationException("select with field names", set1.getType());
    }

}