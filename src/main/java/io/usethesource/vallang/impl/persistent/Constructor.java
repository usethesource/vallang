/*******************************************************************************
 * Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Arnold Lankamp - interfaces and implementation
 *******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import io.usethesource.capsule.util.collection.AbstractSpecialisedImmutableMap;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.impl.fields.AbstractDefaultWithKeywordParameters;
import io.usethesource.vallang.impl.fields.ConstructorWithKeywordParametersFacade;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.visitors.IValueVisitor;

/*package*/ class Constructor {
    private abstract static class AbstractConstructor  implements IConstructor {
        protected final Type constructorType;
        private int hashCode;

        public AbstractConstructor(Type constructorType) {
            this.constructorType = constructorType;
        }

        @Override
        public INode setChildren(IValue[] childArray) {
            return new Node(constructorType.getName(), childArray);
        }

        @Override
        public IConstructor set(String label, IValue arg) {
            return set(constructorType.getFieldIndex(label), arg);
        }

        @Override
        public Iterable<IValue> getChildren(){
            return this;
        }

        @Override
        public IConstructor replace(int first, int second, int end, IList repl) {
            throw new UnsupportedOperationException("Replace not supported on constructor.");
        }

        @Override
        public int hashCode(){
            if (hashCode == 0) {
                hashCode = constructorType.hashCode();

                for (int i = arity() - 1; i >= 0; i--) {
                    hashCode = (hashCode << 23) + (hashCode >> 5);
                    hashCode ^= get(i).hashCode();
                }
            }

            return hashCode;
        }

        @Override
        public boolean equals(@Nullable Object o){
            if(o == this) return true;
            if(o == null) return false;

            if(o.getClass() == getClass()){
                AbstractConstructor otherTree = (AbstractConstructor) o;

                if (constructorType != otherTree.constructorType) {
                    return false;
                }

                Iterator<IValue> children = iterator();
                Iterator<IValue> other = otherTree.iterator();

                while (children.hasNext() && other.hasNext()) {
                    if (!children.next().equals(other.next())) {
                        return false;
                    }
                }

                return true;
            }

            return false;
        }
        
        @Override
        public String toString() {
            return defaultToString();
        }


        @Override
        public boolean has(String label) {
            return getConstructorType().hasField(label);
        }

        @Override
        public IWithKeywordParameters<IConstructor> asWithKeywordParameters() {
            return new AbstractDefaultWithKeywordParameters<IConstructor>(this, AbstractSpecialisedImmutableMap.<String,IValue>mapOf()) {
                @Override
                protected IConstructor wrap(IConstructor content, io.usethesource.capsule.Map.Immutable<String, IValue> parameters) {
                    return new ConstructorWithKeywordParametersFacade(content, parameters);
                }

                @Override
                public boolean hasParameters() {
                    return false;
                }

                @Override
                public java.util.Set<String> getParameterNames() {
                    return Collections.emptySet();
                }

                @Override
                public Map<String, IValue> getParameters() {
                    return Collections.unmodifiableMap(parameters);
                }
            };
        }

        @Override
        public IValue get(String label){
            return get(constructorType.getFieldIndex(label));
        }

        @Override
        public String getName(){
            return constructorType.getName();
        }

        @Override
        public Type getUninstantiatedConstructorType() {
            return constructorType;
        }

        @Override
        public Type getType(){
            return getConstructorType().getAbstractDataType();
        }

        @Override
        public Iterator<IValue> iterator() {
            return new Iterator<IValue>() {
                private final int max = arity();
                private int cur = 0;

                @Override
                public boolean hasNext() {
                    return cur != max;
                }

                @Override
                public IValue next() {
                    IValue res = get(cur);
                    cur++;
                    return res;
                }
            };
        }

        @Override
        public Type getConstructorType(){
            return constructorType;
        }

        @Override
        public Type getChildrenTypes(){
            return constructorType.getFieldTypes();
        }

        @Override
        public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E{
            return v.visitConstructor(this);
        }
    }

    private static class Constructor0 extends AbstractConstructor {
        public Constructor0(Type constructorType) {
            super(constructorType);
        }

        @Override
        public int arity() {
            return 0;
        }

        @Override
        public Iterator<IValue> iterator() {
            return Collections.<IValue>emptyList().iterator();
        }

        @Override
        public int hashCode(){
            return constructorType.hashCode();
        }

        @Override
        public IValue get(int arg0) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public IConstructor set(int arg0, IValue arg1) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public IConstructor set(String arg0, IValue arg1) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return o == this;
        }

        @Override
        public boolean match(IValue value) {
            // either value is the same instance
            // or the other could be a wrapped IConstructor (with keyword params) so compare the constructor type and the arity
            return value == this || (value instanceof IConstructor && (((IConstructor)value).arity() == 0 && ((IConstructor)value).getConstructorType() == this.constructorType));
        }
    }

    private static class Constructor1 extends AbstractConstructor {
        private final IValue arg1;

        public Constructor1(Type constructorType, IValue arg1) {
            super(constructorType);
            this.arg1 = arg1;
        }

        @Override
        public int arity() {
            return 1;
        }

        @Override
        public IValue get(int index) {
            switch (index) {
            case 0: return arg1;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public IConstructor set(int index, IValue newArg) {
            switch (index) {
            case 0: return new Constructor1(constructorType, newArg);
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o.getClass() != getClass()) {
                return false;
            }
            Constructor1 otherTree = (Constructor1) o;

            if (constructorType != otherTree.constructorType) {
                return false;
            }

            return Objects.equals(arg1, otherTree.arg1);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    private static class Constructor2 extends AbstractConstructor {
        private final IValue arg1;
        private final IValue arg2;

        public Constructor2(Type constructorType, IValue arg1, IValue arg2) {
            super(constructorType);
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        @Override
        public int arity() {
            return 2;
        }

        @Override
        public IValue get(int index) {
            switch (index) {
            case 0: return arg1;
            case 1: return arg2;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public IConstructor set(int index, IValue newArg) {
            switch (index) {
            case 0: return new Constructor2(constructorType, newArg, arg2);
            case 1: return new Constructor2(constructorType, arg1, newArg);
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o.getClass() != getClass()) {
                return false;
            }
            Constructor2 otherTree = (Constructor2) o;

            if (constructorType != otherTree.constructorType) {
                return false;
            }

            return Objects.equals(arg1, otherTree.arg1)
                    && Objects.equals(arg2, otherTree.arg2);
        }
    }

    private static class Constructor3 extends AbstractConstructor {
        private final IValue arg1;
        private final IValue arg2;
        private final IValue arg3;

        public Constructor3(Type constructorType, IValue arg1, IValue arg2, IValue arg3) {
            super(constructorType);
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
        }

        @Override
        public int arity() {
            return 3;
        }

        @Override
        public IValue get(int index) {
            switch (index) {
            case 0: return arg1;
            case 1: return arg2;
            case 2: return arg3;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public IConstructor set(int index, IValue newArg) {
            switch (index) {
            case 0: return new Constructor3(constructorType, newArg, arg2, arg3);
            case 1: return new Constructor3(constructorType, arg1, newArg, arg3);
            case 2: return new Constructor3(constructorType, arg1, arg2, newArg);
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o.getClass() != getClass()) {
                return false;
            }
            Constructor3 otherTree = (Constructor3) o;

            if (constructorType != otherTree.constructorType) {
                return false;
            }

            return Objects.equals(arg1, otherTree.arg1)
                    && Objects.equals(arg2, otherTree.arg2)
                    && Objects.equals(arg3, otherTree.arg3);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    private static class Constructor4 extends AbstractConstructor {
        private final IValue arg1;
        private final IValue arg2;
        private final IValue arg3;
        private final IValue arg4;

        public Constructor4(Type constructorType, IValue arg1, IValue arg2, IValue arg3, IValue arg4) {
            super(constructorType);
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
        }

        @Override
        public int arity() {
            return 4;
        }

        @Override
        public IValue get(int index) {
            switch (index) {
            case 0: return arg1;
            case 1: return arg2;
            case 2: return arg3;
            case 3: return arg4;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public IConstructor set(int index, IValue newArg) {
            switch (index) {
            case 0: return new Constructor4(constructorType, newArg, arg2, arg3, arg4);
            case 1: return new Constructor4(constructorType, arg1, newArg, arg3, arg4);
            case 2: return new Constructor4(constructorType, arg1, arg2, newArg, arg4);
            case 3: return new Constructor4(constructorType, arg1, arg2, arg3, newArg);
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o.getClass() != getClass()) {
                return false;
            }
            Constructor4 otherTree = (Constructor4) o;

            if (constructorType != otherTree.constructorType) {
                return false;
            }

            return Objects.equals(arg1, otherTree.arg1)
                    && Objects.equals(arg2, otherTree.arg2)
                    && Objects.equals(arg3, otherTree.arg3)
                    && Objects.equals(arg4, otherTree.arg4);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    private static class Constructor5 extends AbstractConstructor {
        private final IValue arg1;
        private final IValue arg2;
        private final IValue arg3;
        private final IValue arg4;
        private final IValue arg5;

        public Constructor5(Type constructorType, IValue arg1, IValue arg2, IValue arg3, IValue arg4, IValue arg5) {
            super(constructorType);
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
        }

        @Override
        public int arity() {
            return 5;
        }

        @Override
        public IValue get(int index) {
            switch (index) {
            case 0: return arg1;
            case 1: return arg2;
            case 2: return arg3;
            case 3: return arg4;
            case 4: return arg5;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public IConstructor set(int index, IValue newArg) {
            switch (index) {
            case 0: return new Constructor5(constructorType, newArg, arg2, arg3, arg4, arg5);
            case 1: return new Constructor5(constructorType, arg1, newArg, arg3, arg4, arg5);
            case 2: return new Constructor5(constructorType, arg1, arg2, newArg, arg4, arg5);
            case 3: return new Constructor5(constructorType, arg1, arg2, arg3, newArg, arg5);
            case 4: return new Constructor5(constructorType, arg1, arg2, arg3, arg4, newArg);
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o.getClass() != getClass()) {
                return false;
            }
            Constructor5 otherTree = (Constructor5) o;

            if (constructorType != otherTree.constructorType) {
                return false;
            }

            return Objects.equals(arg1, otherTree.arg1)
                    && Objects.equals(arg2, otherTree.arg2)
                    && Objects.equals(arg3, otherTree.arg3)
                    && Objects.equals(arg4, otherTree.arg4)
                    && Objects.equals(arg5, otherTree.arg5);
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    private static class Constructor6 extends AbstractConstructor {
        private final IValue arg1;
        private final IValue arg2;
        private final IValue arg3;
        private final IValue arg4;
        private final IValue arg5;
        private final IValue arg6;

        public Constructor6(Type constructorType, IValue arg1, IValue arg2, IValue arg3, IValue arg4, IValue arg5, IValue arg6) {
            super(constructorType);
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
            this.arg6 = arg6;
        }

        @Override
        public int arity() {
            return 6;
        }

        @Override
        public IValue get(int index) {
            switch (index) {
            case 0: return arg1;
            case 1: return arg2;
            case 2: return arg3;
            case 3: return arg4;
            case 4: return arg5;
            case 5: return arg6;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public IConstructor set(int index, IValue newArg) {
            switch (index) {
            case 0: return new Constructor6(constructorType, newArg, arg2, arg3, arg4, arg5, arg6);
            case 1: return new Constructor6(constructorType, arg1, newArg, arg3, arg4, arg5, arg6);
            case 2: return new Constructor6(constructorType, arg1, arg2, newArg, arg4, arg5, arg6);
            case 3: return new Constructor6(constructorType, arg1, arg2, arg3, newArg, arg5, arg6);
            case 4: return new Constructor6(constructorType, arg1, arg2, arg3, arg4, newArg, arg6);
            case 5: return new Constructor6(constructorType, arg1, arg2, arg3, arg4, arg5, newArg);
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o.getClass() != getClass()) {
                return false;
            }
            Constructor6 otherTree = (Constructor6) o;

            if (constructorType != otherTree.constructorType) {
                return false;
            }

            return Objects.equals(arg1, otherTree.arg1)
                    && Objects.equals(arg2, otherTree.arg2)
                    && Objects.equals(arg3, otherTree.arg3)
                    && Objects.equals(arg4, otherTree.arg4)
                    && Objects.equals(arg5, otherTree.arg5)
                    && Objects.equals(arg6, otherTree.arg6);
        }
    }

    private static class Constructor7 extends AbstractConstructor {
        private final IValue arg1;
        private final IValue arg2;
        private final IValue arg3;
        private final IValue arg4;
        private final IValue arg5;
        private final IValue arg6;
        private final IValue arg7;

        public Constructor7(Type constructorType, IValue arg1, IValue arg2, IValue arg3, IValue arg4, IValue arg5, IValue arg6, IValue arg7) {
            super(constructorType);
            this.arg1 = arg1;
            this.arg2 = arg2;
            this.arg3 = arg3;
            this.arg4 = arg4;
            this.arg5 = arg5;
            this.arg6 = arg6;
            this.arg7 = arg7;
        }

        @Override
        public int arity() {
            return 7;
        }

        @Override
        public IValue get(int index) {
            switch (index) {
            case 0: return arg1;
            case 1: return arg2;
            case 2: return arg3;
            case 3: return arg4;
            case 4: return arg5;
            case 5: return arg6;
            case 6: return arg7;
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public IConstructor set(int index, IValue newArg) {
            switch (index) {
            case 0: return new Constructor7(constructorType, newArg, arg2, arg3, arg4, arg5, arg6, arg7);
            case 1: return new Constructor7(constructorType, arg1, newArg, arg3, arg4, arg5, arg6, arg7);
            case 2: return new Constructor7(constructorType, arg1, arg2, newArg, arg4, arg5, arg6, arg7);
            case 3: return new Constructor7(constructorType, arg1, arg2, arg3, newArg, arg5, arg6, arg7);
            case 4: return new Constructor7(constructorType, arg1, arg2, arg3, arg4, newArg, arg6, arg7);
            case 5: return new Constructor7(constructorType, arg1, arg2, arg3, arg4, arg5, newArg, arg7);
            case 6: return new Constructor7(constructorType, arg1, arg2, arg3, arg4, arg5, arg6, newArg);
            default:
                throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
        
        @Override
        public boolean equals(@Nullable Object o) {
            if (o == this) {
                return true;
            }
            if (o == null) {
                return false;
            }

            if (o.getClass() != getClass()) {
                return false;
            }
            Constructor7 otherTree = (Constructor7) o;

            if (constructorType != otherTree.constructorType) {
                return false;
            }

            return Objects.equals(arg1, otherTree.arg1)
                    && Objects.equals(arg2, otherTree.arg2)
                    && Objects.equals(arg3, otherTree.arg3)
                    && Objects.equals(arg4, otherTree.arg4)
                    && Objects.equals(arg5, otherTree.arg5)
                    && Objects.equals(arg6, otherTree.arg6)
                    && Objects.equals(arg7, otherTree.arg7);
        }
    }

    private static class ConstructorN extends AbstractConstructor {
        protected final IValue[] children;

        public ConstructorN(Type constructorType, IValue[] children) {
            super(constructorType);
            this.children = children;
        }

        @Override
        public int arity(){
            return children.length;
        }

        @Override
        public Iterator<IValue> iterator() {
            return Arrays.stream(children).iterator();
        }

        @Override
        public IValue get(int i){
            return children[i];
        }

        @Override
        public IConstructor set(int index, IValue newArg) {
            IValue[] newChildren = new IValue[children.length];
            System.arraycopy(children, 0, newChildren, 0, children.length);
            newChildren[index] = newArg;
            return new ConstructorN(constructorType, newChildren);
        }
    }

    private static class TypeParameterizedConstructorN extends ConstructorN {
        private final Type uninstantiatedConstructorType;

        public TypeParameterizedConstructorN(Type constructorType, IValue[] children) {
            super(instantiate(constructorType, children), children);
            assert constructorType.isParameterized();
            this.uninstantiatedConstructorType = constructorType;
        }

        private static Type instantiate(Type constructorType, IValue[] children) {
            Type[] actualTypes = new Type[constructorType.getArity()];
            int i = 0;
            for (IValue child : children) {
                actualTypes[i++] = child.getType();
            }

            Map<Type,Type> bindings = new HashMap<Type,Type>();
            constructorType.getFieldTypes().match(TypeFactory.getInstance().tupleType(actualTypes), bindings);

            for (Type field : constructorType.getAbstractDataType().getTypeParameters()) {
                if (!bindings.containsKey(field)) {
                    bindings.put(field, TypeFactory.getInstance().voidType());
                }
            }

            return constructorType.instantiate(bindings);
        }

        @Override
        public Type getUninstantiatedConstructorType() {
            return uninstantiatedConstructorType;
        }
    }

    /**
     * As empty constructors are very common and are only based on a type that is already maximally shared we also maximally share the Constructor0 instances
     * 
     * This descreases both memory footprint and allocation overhead.
     */
    private static final LoadingCache<Type, IConstructor> EMPTY_CONSTRUCTOR_SINGLETONS = Caffeine.newBuilder().build(Constructor0::new);

    /*package*/ static IConstructor newConstructor(Type constructorType, IValue[] children) {
        assert IConstructor.assertTypeCorrectConstructorApplication(constructorType, children);
        
        if (constructorType.isParameterized()) {
            return new TypeParameterizedConstructorN(constructorType, children);
        }

        switch (children.length) {
        case 0: return nonNull(EMPTY_CONSTRUCTOR_SINGLETONS.get(constructorType));
        case 1: return new Constructor1(constructorType, children[0]);
        case 2: return new Constructor2(constructorType, children[0], children[1]);
        case 3: return new Constructor3(constructorType, children[0], children[1], children[2]);
        case 4: return new Constructor4(constructorType, children[0], children[1], children[2], children[3]);
        case 5: return new Constructor5(constructorType, children[0], children[1], children[2], children[3], children[4]);
        case 6: return new Constructor6(constructorType, children[0], children[1], children[2], children[3], children[4], children[5]);
        case 7: return new Constructor7(constructorType, children[0], children[1], children[2], children[3], children[4], children[5], children[6]);
        default: return new ConstructorN(constructorType, children);
        }
    }
    private static <T> T nonNull(@Nullable T value) {
        if (value == null) {
            throw new RuntimeException("Unexpected null value");
        }
        return value;
    }
    
    /*package*/ static IConstructor newConstructor(Type constructorType, IValue[] children, Map<String,IValue> kwParams) {
        IConstructor r = newConstructor(constructorType, children);

        if (kwParams != null && !kwParams.isEmpty()) {
            return r.asWithKeywordParameters().setParameters(kwParams);
        }

        return r;
    }
}
