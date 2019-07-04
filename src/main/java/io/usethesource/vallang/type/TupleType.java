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

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.exceptions.UndeclaredFieldException;
import org.checkerframework.checker.nullness.qual.Nullable;

/*package*/final class TupleType extends DefaultSubtypeOfValue {
    protected final Type[] fFieldTypes; // protected access for the benefit of inner classes
    protected final @Nullable String[] fFieldNames;
    protected int fHashcode = -1;

    /**
     * Creates a tuple type with the given field types. Copies the array.
     */
    /* package */TupleType(Type[] fieldTypes) {
        fFieldTypes = fieldTypes; // fieldTypes.clone(); was safer, but it ended
        // up being a bottleneck
        fFieldNames = null;
    }

    /**
     * Creates a tuple type with the given field types and names. Copies the
     * arrays.
     */
    /* package */TupleType(Type[] fieldTypes, String[] fieldNames) {
        fFieldTypes = fieldTypes; // fieldTypes.clone(); was safer, but it ended
        // up being a bottleneck
        if (fieldNames.length != 0) {
            fFieldNames = fieldNames; // fieldNames.clone(); same here
        } else {
            fFieldNames = null;
        }
    }

    public static class Info implements TypeFactory.TypeReifier {
        @Override
        public Type getSymbolConstructorType() {
            return symbols().typeSymbolConstructor("tuple", TF.listType(symbols().symbolADT()), "symbols");
        }

        @Override
        public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor, Set<IConstructor>> grammar) {
            return symbols().fromSymbols((IList) symbol.get("symbols"), store, grammar);
        }

        @Override
        public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                Set<IConstructor> done) {
            IListWriter w = vf.listWriter();

            if (type.hasFieldNames()) {
                for (int i = 0; i < type.getArity(); i++) {
                    w.append(symbols().labelSymbol(vf, type.getFieldType(i).asSymbol(vf, store, grammar, done), type.getFieldName(i)));
                }
            }
            else {
                for (Type f : type) {
                    w.append(f.asSymbol(vf, store, grammar, done));
                }
            }

            return vf.constructor(getSymbolConstructorType(), w.done());
        }

        @Override
        public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
                Set<IConstructor> done) {
            for (Type f : type) {
                f.asProductions(vf, store, grammar, done);
            }
        }

        @Override
        public boolean isRecursive() {
            return true;
        }

        @Override
        public Type randomInstance(Supplier<Type> next, TypeStore store, Random rnd) {
            return randomInstance(next, store, rnd, rnd.nextInt(5));
        }

        @SuppressWarnings("deprecation")
        public Type randomInstance(Supplier<Type> next, TypeStore store, Random rnd, int arity) {
            Type[] types = new Type[arity];

            for (int i = 0; i < arity; i++) {
                types[i] = next.get();
            }

            if (rnd.nextBoolean()) {
                return tf().tupleType(types);
            }

            String[] labels = new String[arity];
            for (int i = 0; i < arity; i++) {
                labels[i] = randomLabel(rnd);
            }

            return tf().tupleType(types, labels);
        }


    }

    @Override
    public TypeFactory.TypeReifier getTypeReifier() {
        return new Info();
    }

    @Override
    public boolean hasFieldNames() {
        return fFieldNames != null;
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
        if (fFieldNames != null) {
            for (int i = fFieldNames.length - 1; i >= 0; i--) {
                if (fFieldNames[i].equals(fieldName)) {
                    return i;
                }
            }
        }

        throw new UndeclaredFieldException(this, fieldName);
    }

    @Override
    public boolean hasField(String fieldName) {
        if (fFieldNames != null) {
            for (int i = fFieldNames.length - 1; i >= 0; i--) {
                if (fFieldNames[i].equals(fieldName)) {
                    return true;
                }
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

        if (hasFieldNames() && other.hasFieldNames()) {
            String fieldNameLeft = this.getFieldName(0);
            String fieldNameRight = other.getFieldName(1);

            if (!fieldNameLeft.equals(fieldNameRight)) {
                return TF.tupleType(this.getFieldType(0), fieldNameLeft,
                        other.getFieldType(1), fieldNameRight);
            }
        }

        return TF.tupleType(this.getFieldType(0), other.getFieldType(1));
    }

    @Override
    public Type carrier() {
        Type lub = TypeFactory.getInstance().voidType();

        for (Type field : this) {
            lub = lub.lub(field);
        }

        return TypeFactory.getInstance().setType(lub);
    }

    @Override
    public Type getFieldTypes() {
        return this;
    }

    @Override
    public Type closure() {
        if (getArity() == 2) {
            Type lub = fFieldTypes[0].lub(fFieldTypes[1]);
            return TF.tupleType(lub, lub); 
        }
        return super.closure();
    }

    /**
     * Compute a new tupletype that is the lub of t1 and t2. Precondition: t1
     * and t2 have the same arity.
     * 
     * @param t1
     * @param t2
     * @return a TupleType which is the lub of t1 and t2
     */
    @SuppressWarnings("deprecation")
    static Type lubTupleTypes(Type t1, Type t2) {
        int N = t1.getArity();
        Type[] fieldTypes = new Type[N];
        String[] fieldNames = new String[N];

        for (int i = 0; i < N; i++) {
            fieldTypes[i] = t1.getFieldType(i).lub(t2.getFieldType(i));

            if (t1.hasFieldNames()) {
                fieldNames[i] = t1.getFieldName(i);
            } else if (t2.hasFieldNames()) {
                fieldNames[i] = t2.getFieldName(i);
            }
        }

        if(t1.hasFieldNames() || t2.hasFieldNames()) {
            return TypeFactory.getInstance().tupleType(fieldTypes, fieldNames);
        }
        else {
            return TypeFactory.getInstance().tupleType(fieldTypes);
        }
    }

    /**
     * Compute a new tupletype that is the glb of t1 and t2. Precondition: t1
     * and t2 have the same arity.
     * 
     * @param t1
     * @param t2
     * @return a TupleType which is the glb of t1 and t2
     */
    @SuppressWarnings("deprecation")
    static Type glbTupleTypes(Type t1, Type t2) {
        int N = t1.getArity();
        Type[] fieldTypes = new Type[N];
        String[] fieldNames = new String[N];

        for (int i = 0; i < N; i++) {
            fieldTypes[i] = t1.getFieldType(i).glb(t2.getFieldType(i));

            if (t1.hasFieldNames()) {
                fieldNames[i] = t1.getFieldName(i);
            } else if (t2.hasFieldNames()) {
                fieldNames[i] = t2.getFieldName(i);
            }
        }

        if(t1.hasFieldNames() || t2.hasFieldNames()) {
            return TypeFactory.getInstance().tupleType(fieldTypes, fieldNames);
        }
        else {
            return TypeFactory.getInstance().tupleType(fieldTypes);
        }
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
    static Type lubNamedTupleTypes(Type t1, Type t2) {
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

        if (consistent && first && second) {
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
    static Type glbNamedTupleTypes(Type t1, Type t2) {
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
        int h = fHashcode;
        if (h == -1) {
            h = 55501;
            for (Type elemType : fFieldTypes) {
                h = h * 44927 + elemType.hashCode();
            }
            fHashcode = h;
        }
        return h;
    }

    /**
     * Compute tuple type equality. Note that field labels are significant here
     * for equality while they do not count for isSubtypeOf and lub.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof TupleType)) {
            return false;
        }

        TupleType other = (TupleType) obj;
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
        }

        if (fFieldNames != null) {
            if (other.fFieldNames == null) {
                return false;
            }
            for (int i = fFieldNames.length - 1; i >= 0; i--) {
                if (!fFieldNames[i].equals(other.fFieldNames[i])) {
                    return false;
                }
            }
        } else if (other.fFieldNames != null) {
            return false;
        }

        return true;
}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("tuple[");
        int idx = 0;
        for (Type elemType : fFieldTypes) {
            if (idx++ > 0)
                sb.append(",");
            sb.append(elemType.toString());
            if (hasFieldNames()) {
                sb.append(" " + fFieldNames[idx - 1]);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public Iterator<Type> iterator() {
        return new Iterator<Type>() {
            private int cursor = 0;

            public boolean hasNext() {
                return cursor < fFieldTypes.length;
            }

            public Type next() {
                return fFieldTypes[cursor++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitTuple(this);
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
        return type.isSubtypeOfTuple(this);
    }

    @Override
    public Type lub(Type other) {
        return other.lubWithTuple(this);
    }

    @Override
    public Type glb(Type type) {
        return type.glbWithTuple(this);
    }

    @Override
    protected boolean isSubtypeOfTuple(Type type) {
        if (getArity() == type.getArity()) {
            for (int i = 0; i < getArity(); i++) {
                if (!getFieldType(i).isSubtypeOf(type.getFieldType(i))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    @Override
    protected Type lubWithTuple(Type type) {
        if (getArity() == type.getArity()) {
            if(hasFieldNames() && type.hasFieldNames())
                return TupleType.lubNamedTupleTypes(this, type);
            else
                return TupleType.lubTupleTypes(this, type);
        }

        return TF.valueType();
    }

    @Override
    protected Type glbWithTuple(Type type) {
        if (getArity() == type.getArity()) {
            if(hasFieldNames() && type.hasFieldNames())
                return TupleType.glbNamedTupleTypes(this, type);
            else
                return TupleType.glbTupleTypes(this, type);
        }

        return TF.voidType();
    }

    @Override
    public boolean isOpen() {
        for (Type arg : fFieldTypes) {
            if (arg.isOpen()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getFieldName(int i) {
        if (fFieldNames == null) {
            throw new UnsupportedOperationException("this tuple type does not have field names");
        }
        
        return fFieldNames[i];
    }

    @Override
    public String[] getFieldNames(){
        if (fFieldNames == null) {
            throw new UnsupportedOperationException("this tuple type does not have field names");
        }
        
        return fFieldNames;
    }

    protected boolean sameFieldNamePrefix (String[] o){
        if(getArity() > o.length)
            return false;
        for(int i = getArity() - 1; i >= 0; i--)
            if(!fFieldNames[i].equals(o[i]))
                return false;
        return true;
    }

    @Override
    public boolean match(Type matched, Map<Type, Type> bindings)
            throws FactTypeUseException {
        if (!super.match(matched, bindings)) {
            return false;
        }

        for (int i = getArity() - 1; i >= 0; i--) {
            if (!getFieldType(i).match(matched.getFieldType(i), bindings)) {
                return false;
            }
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        if (hasFieldNames()) {
            Type[] fTypes = new Type[getArity()];
            String[] fLabels = new String[getArity()];

            for (int i = fTypes.length - 1; i >= 0; i--) {
                fTypes[i] = getFieldType(i).instantiate(bindings);
                fLabels[i] = getFieldName(i);
            }

            return TypeFactory.getInstance().tupleType(fTypes, fLabels);
        }

        Type[] fChildren = new Type[getArity()];
        for (int i = fChildren.length - 1; i >= 0; i--) {
            fChildren[i] = getFieldType(i).instantiate(bindings);
        }

        return TypeFactory.getInstance().tupleType(fChildren);
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
            if (!hasFieldNames()) {
                Type[] fieldTypes = new Type[width];
                for (int i = width - 1; i >= 0; i--) {
                    fieldTypes[i] = getFieldType(fields[i]);
                }

                return TypeFactory.getInstance().tupleType(fieldTypes);
            }

            Type[] fieldTypes = new Type[width];
            String[] fieldNames = new String[width];
            boolean seenDuplicate = false;

            for (int i = width - 1; i >= 0; i--) {
                fieldTypes[i] = getFieldType(fields[i]);
                fieldNames[i] = getFieldName(fields[i]);

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
