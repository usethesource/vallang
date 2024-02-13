/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *    Jurgen Vinju (Jurgen.Vinju@cwi.nl) - evolution and maintenance
 *******************************************************************************/
package io.usethesource.vallang.type;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeValues;

/*package*/ class TupleType extends DefaultSubtypeOfValue {
    final Type[] fFieldTypes; // protected access for the benefit of inner classes
    protected int fHashcode = -1;

    /**
     * Creates a tuple type with the given field types. Does not copy the array..
     */
    /*package*/ TupleType(Type[] fieldTypes) {
        fFieldTypes = fieldTypes; // fieldTypes.clone(); was safer, but it ended up being a bottleneck
    }

    public static class Info extends TypeFactory.TypeReifier {
        public Info(TypeValues symbols) {
            super(symbols);
        }

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
        public Type randomInstance(Supplier<Type> next, TypeStore store, RandomTypesConfig rnd) {
            return randomInstance(next, rnd, rnd.nextInt(rnd.getMaxDepth() + 1));
        }

        @SuppressWarnings("deprecation")
        /*package*/ Type randomInstance(Supplier<Type> next, RandomTypesConfig rnd, int arity) {
            Type[] types = new Type[arity];

            for (int i = 0; i < arity; i++) {
                while ((types[i] = next.get()).isBottom()); // tuples can not have empty fields
            }

            if (!rnd.isWithTupleFieldNames() || rnd.nextBoolean()) {
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
    public TypeFactory.TypeReifier getTypeReifier(TypeValues symbols) {
        return new Info(symbols);
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
    public int getArity() {
        return fFieldTypes.length;
    }

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
    public String[] getFieldNames() {
        return new String[0];
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
    static Type lubTupleTypes(Type t1, Type t2) {
        int N = t1.getArity();
        Type[] fieldTypes = new Type[N];

        for (int i = 0; i < N; i++) {
            fieldTypes[i] = t1.getFieldType(i).lub(t2.getFieldType(i));
        }

        return TypeFactory.getInstance().tupleType(fieldTypes);
    }

    /**
     * Compute a new tupletype that is the glb of t1 and t2. Precondition: t1
     * and t2 have the same arity.
     * 
     * @param t1
     * @param t2
     * @return a TupleType which is the glb of t1 and t2
     */
    static Type glbTupleTypes(Type t1, Type t2) {
        int N = t1.getArity();
        Type[] fieldTypes = new Type[N];

        for (int i = 0; i < N; i++) {
            fieldTypes[i] = t1.getFieldType(i).glb(t2.getFieldType(i));
        }

        return TypeFactory.getInstance().tupleType(fieldTypes);
    }
    
    @Override
    public boolean hasFieldNames() {
        return false;
    }
    
    @Override
    public boolean hasField(String fieldName) {
        return false;
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

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (obj == this) {
            return true;
        }
        
        if (!obj.getClass().equals(getClass())) {
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
    public boolean intersects(Type other) {
        return other.intersectsWithTuple(this);
    }
    
    @Override
    protected boolean intersectsWithTuple(Type type) {
        int N = this.getArity();
        
        if (N != type.getArity()) {
            return false;
        }
        
        for (int i = 0; i < N; i++) {
            if (!this.getFieldType(i).intersects(type.getFieldType(i))) {
                return false;
            }
        }
        
        return true;
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
    protected boolean isSubtypeOfVoid(Type type) {
        // this can happen if one of the elements is a type parameter which 
        // might degenerate to void.
        if (isOpen()) {
            for (Type elem : this) {
                if (elem.isSubtypeOfVoid(type)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected Type lubWithTuple(Type type) {
        if (getArity() == type.getArity()) {
            return TupleType.lubTupleTypes(this, type);
        }

        return TF.valueType();
    }

    @Override
    protected Type glbWithTuple(Type type) {
        if (getArity() == type.getArity()) {
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
    public boolean match(Type matched, Map<Type, Type> bindings)
            throws FactTypeUseException {
        if (!super.match(matched, bindings)) {
            return false;
        }

        if (matched.isTuple() || (matched.isAliased() && matched.getAliased().isTuple()) || matched.isBottom()) {
            for (int i = getArity() - 1; i >= 0; i--) {
                if (!getFieldType(i).match(matched.getFieldType(i), bindings)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public Type instantiate(Map<Type, Type> bindings) {
        Type[] fChildren = new Type[getArity()];
        for (int i = fChildren.length - 1; i >= 0; i--) {
            fChildren[i] = getFieldType(i).instantiate(bindings);
        }

        return TypeFactory.getInstance().tupleType(fChildren);
    }

    @Override
    public Type select(int... fields) {
        int width = fields.length;

        if (width == 0) {
            return TypeFactory.getInstance().voidType();
        } else if (width == 1) {
            return getFieldType(fields[0]);
        } else {
            Type[] fieldTypes = new Type[width];
            for (int i = width - 1; i >= 0; i--) {
                fieldTypes[i] = getFieldType(fields[i]);
            }

            return TypeFactory.getInstance().tupleType(fieldTypes);
        }
    }
    
    @Override
    public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters, int maxDepth, int maxWidth) {
        IValue[] elems = new IValue[getArity()];
        
        for (int i = 0; i < elems.length; i++) {
            assert !getFieldType(i).isBottom() : "field " + i + " has illegal type void";
            elems[i] = getFieldType(i).randomValue(random, vf, store, typeParameters, maxDepth - 1, maxWidth);
        }
        
        ITuple done = vf.tuple(elems);
        match(done.getType(), typeParameters);
        
        return done;
    }
    
    @Override
    public boolean isTuple() {
        return true;
    }
}
