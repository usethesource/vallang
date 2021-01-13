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

package io.usethesource.vallang.type;

import java.util.Map;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.exceptions.UndeclaredFieldException;

/*package*/ final class TupleTypeWithFieldNames extends TupleType {
    final String[] fFieldNames;

    /**
     * Creates a tuple type with the given field types and names. 
     * Does not copy the arrays.
     */
    /*package*/ TupleTypeWithFieldNames(Type[] fieldTypes, String[] fieldNames) {
        super(fieldTypes);
        assert fieldTypes.length != 0 : "nullary tuples should be instances of TupleType without field names";
        fFieldNames = fieldNames; // fieldNames.clone() was a bottleneck
    }

    @Override
    public boolean hasFieldNames() {
        return true;
    }

    @Override
    public boolean isFixedWidth() {
        return true;
    }

    @Override
    public Type getFieldType(int i) {
        return fFieldTypes[i];
    }

    @Override
    public Type getFieldType(String fieldName) {
        return getFieldType(getFieldIndex(fieldName));
    }

    @Override
    public int getFieldIndex(String fieldName) throws FactTypeUseException {
        for (int i = fFieldNames.length - 1; i >= 0; i--) {
            if (fFieldNames[i].equals(fieldName)) {
                return i;
            }
        }

        throw new UndeclaredFieldException(this, fieldName);
    }

    @Override
    public boolean hasField(String fieldName) {
        for (int i = fFieldNames.length - 1; i >= 0; i--) {
            if (fFieldNames[i].equals(fieldName)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getArity() {
        return fFieldTypes.length;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Type compose(Type other) {
        if (other.equivalent(TF.voidType())) {
            return other;
        }

        if (this.getArity() != 2 || other.getArity() != 2) {
            throw new IllegalOperationException("compose", this, other);
        }

        if (!getFieldType(1).comparable(other.getFieldType(0))) {
            return TF.voidType(); // since nothing will be composable
        }

        if (other.hasFieldNames()) {
            String fieldNameLeft = this.getFieldName(0);
            String fieldNameRight = other.getFieldName(1);

            if (!fieldNameLeft.equals(fieldNameRight)) {
                return TF.tupleType(this.getFieldType(0), fieldNameLeft,
                        other.getFieldType(1), fieldNameRight);
            }
        }

        return TF.tupleType(this.getFieldType(0), other.getFieldType(1));
    }

    /**
     * Compute a new tupleType that is the lub of t1 and t2. Precondition: t1
     * and t2 have the same arity.
     * 
     * @param t1
     * @param t2
     * @return a TupleType which is the lub of t1 and t2, if all the names are
     *         equal at every position, they remain, otherwise we get an
     *         unlabeled tuple.
     */
    @SuppressWarnings("deprecation")
    private static Type lubNamedTupleTypes(Type t1, Type t2) {
        int N = t1.getArity();
        Object[] fieldTypes = new Object[N * 2];
        Type[] types = new Type[N];
        boolean first = t1.hasFieldNames();
        boolean second = t2.hasFieldNames();
        boolean consistent = true;

        for (int i = 0, j = 0; i < N; i++, j++) {
            Type lub = t1.getFieldType(i).lub(t2.getFieldType(i));
            types[i] = lub;
            fieldTypes[j++] = lub;

            if (first && second) {
                String fieldName1 = t1.getFieldName(i);
                String fieldName2 = t2.getFieldName(i);

                if (fieldName1.equals(fieldName2)) {
                    fieldTypes[j] = fieldName1;
                } else {
                    consistent = false;
                }
            } else if (first) {
                fieldTypes[j] = t1.getFieldName(i);
            } else if (second) {
                fieldTypes[i] = t2.getFieldName(i);
            }
        }

        if (consistent && (first || second)) {
            return TypeFactory.getInstance().tupleType(fieldTypes);
        } else {
            return TypeFactory.getInstance().tupleType(types);
        }
    }

    /**
     * Compute a new tupletype that is the glb of t1 and t2. Precondition: t1
     * and t2 have the same arity.
     * 
     * @param t1
     * @param t2
     * @return a TupleType which is the glb of t1 and t2, if all the names are
     *         equal at every position, they remain, otherwise we get an
     *         unlabeled tuple.
     */
    @SuppressWarnings("deprecation")
    private static Type glbNamedTupleTypes(Type t1, Type t2) {
        int N = t1.getArity();
        Object[] fieldTypes = new Object[N * 2];
        Type[] types = new Type[N];
        boolean first = t1.hasFieldNames();
        boolean second = t2.hasFieldNames();
        boolean consistent = true;

        for (int i = 0, j = 0; i < N; i++, j++) {
            Type lub = t1.getFieldType(i).glb(t2.getFieldType(i));
            types[i] = lub;
            fieldTypes[j++] = lub;

            if (first && second) {
                String fieldName1 = t1.getFieldName(i);
                String fieldName2 = t2.getFieldName(i);

                if (fieldName1.equals(fieldName2)) {
                    fieldTypes[j] = fieldName1;
                } else {
                    consistent = false;
                }
            } else if (first) {
                fieldTypes[j] = t1.getFieldName(i);
            } else if (second) {
                fieldTypes[i] = t2.getFieldName(i);
            }
        }

        if (consistent && first && second) {
            return TypeFactory.getInstance().tupleType(fieldTypes);
        } else {
            return TypeFactory.getInstance().tupleType(types);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Compute tuple type equality. Note that field labels are significant here
     * for equality while they do not count for isSubtypeOf and lub.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (!obj.getClass().equals(getClass())) {
            return false;
        }

        TupleTypeWithFieldNames other = (TupleTypeWithFieldNames) obj;
        if (fFieldTypes.length != other.fFieldTypes.length) {
            return false;
        }

        for (int i = fFieldTypes.length - 1; i >= 0; i--) {
            // N.B.: The field types must have been created and canonicalized
            // before any
            // attempt to manipulate the outer type (i.e. TupleType), so we can
            // use object
            // identity here for the fFieldTypes.
            if (fFieldTypes[i] != other.fFieldTypes[i]) {
                return false;
            }
            
            if (!fFieldNames[i].equals(other.fFieldNames[i])) {
                return false;
            }
        }

        return true;
}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("tuple[");
        int idx = 0;
        for (Type elemType : fFieldTypes) {
            if (idx++ > 0) {
                sb.append(",");
            }
            sb.append(elemType.toString());
            sb.append(" " + Objects.requireNonNull(fFieldNames)[idx - 1]);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    protected Type lubWithTuple(Type type) {
        if (getArity() == type.getArity()) {
            if (type.hasFieldNames() || this.hasFieldNames()) {
                return TupleTypeWithFieldNames.lubNamedTupleTypes(this, type);
            }
            else {
                return super.lubWithTuple(type);
            }
        }

        return TF.valueType();
    }
    
    @Override
    protected Type glbWithTuple(Type type) {
        if (getArity() == type.getArity()) {
            if (type.hasFieldNames()) {
                return TupleTypeWithFieldNames.glbNamedTupleTypes(this, type);
            }
            else {
                return super.glbWithTuple(type);
            }
        }

        return TF.voidType();
    }

    @Override
    @Pure
    public String getFieldName(int i) {
        return fFieldNames[i];
    }

    @Override
    @Pure
    public String[] getFieldNames() {
        return fFieldNames;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        Type[] fTypes = new Type[getArity()];
        String[] fLabels = new String[getArity()];

        for (int i = fTypes.length - 1; i >= 0; i--) {
            fTypes[i] = getFieldType(i).instantiate(bindings);
            fLabels[i] = getFieldName(i);
        }

        return TypeFactory.getInstance().tupleType(fTypes, fLabels);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Type select(int... fields) {
        int width = fields.length;

        if (width == 0) {
            return TypeFactory.getInstance().voidType();
        } else if (width == 1) {
            return getFieldType(fields[0]);
        } else {
            Type[] fieldTypes = new Type[width];
            String[] fieldNames = new String[width];
            boolean seenDuplicate = false;

            for (int i = width - 1; i >= 0; i--) {
                fieldTypes[i] = getFieldType(fields[i]);
                fieldNames[i] = Objects.requireNonNull(getFieldName(fields[i]));

                for (int j = width - 1; j > i; j--) {
                    if (fieldNames[j].equals(fieldNames[i])) {
                        seenDuplicate = true;
                    }
                }
            }

            if (!seenDuplicate) {
                return TypeFactory.getInstance().tupleType(fieldTypes, fieldNames);
            } else {
                return TypeFactory.getInstance().tupleType(fieldTypes);
            }
        }
    }

    @Override
    public Type select(String... names) {
        int[] indexes = new int[names.length];
        int i = 0;
        for (String name : names) {
            indexes[i] = getFieldIndex(name);
        }

        return select(indexes);
    }
}
