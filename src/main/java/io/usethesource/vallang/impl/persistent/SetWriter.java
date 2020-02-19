/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWriter;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.util.AbstractTypeBag;

/*
 * TODO: visibility is currently public to allow set-multimap experiments. Must be set back to
 * `protected` when experiments are finished.
 */
public class SetWriter implements ISetWriter {

    public static final boolean USE_MULTIMAP_BINARY_RELATIONS = true;


    public static Predicate<Type> isTuple = (type) -> type.isTuple();
    public static Predicate<Type> arityEqualsTwo = (type) -> type.getArity() == 2;
    public static Predicate<Type> isTupleOfArityTwo = isTuple.and(arityEqualsTwo);


    protected AbstractTypeBag elementTypeBag;

    protected @MonotonicNonNull ISet constructedSet;

    private Type leastUpperBound = TypeFactory.getInstance().voidType();

    private @MonotonicNonNull Builder builder;

    private final BiFunction<IValue, IValue, ITuple> constructTuple;

    private static interface Builder extends Iterable<IValue> {
        void put(IValue element, Type elementType);
        ISet done();
    }

    private final static class SetBuilder implements Builder {
        private final Set.Transient<IValue> set = Set.Transient.of();
        private AbstractTypeBag elementTypeBag = AbstractTypeBag.of();

        @Override
        public void put(IValue element, Type elementType) {
            if (set.__insert(element)) {
                elementTypeBag = elementTypeBag.increase(elementType);
            }
        }

        @Override
        public ISet done() {
            return PersistentSetFactory.from(elementTypeBag, set.freeze());
        }

        @Override
        public Iterator<IValue> iterator() {
            return set.iterator();
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("{");
            for (IValue e : this) {
                b.append(e.toString());
                b.append(",");
            }
            b.append("}");

            return b.toString();
        }
    }

    private final static class MultiMapBuilder implements Builder {
        AbstractTypeBag keyTypeBag = AbstractTypeBag.of();
        AbstractTypeBag valTypeBag = AbstractTypeBag.of();
        SetMultimap.Transient<IValue, IValue> map = SetMultimap.Transient.of();

        @Override
        public void put(IValue element, Type elementType) {
            IValue key = ((ITuple)element).get(0);
            IValue value = ((ITuple)element).get(1);
            if (map.__insert(key, value)) {
                keyTypeBag = keyTypeBag.increase(elementType.getFieldType(0));
                valTypeBag = valTypeBag.increase(elementType.getFieldType(1));
            }
        }

        @Override
        public ISet done() {
            return PersistentSetFactory.from(keyTypeBag, valTypeBag, map.freeze());
        }

        @Override
        public Iterator<IValue> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("{");
            Iterator<IValue> keys = map.keyIterator();
            while (keys.hasNext()) {
                IValue key = keys.next();

                for (IValue val : map.get(key)) {
                    b.append("<");
                    b.append(key);
                    b.append(",");
                    b.append(val);
                    b.append(">");
                }
            }
            b.append("}");

            return b.toString();
        }
    }

    /*package*/ SetWriter(BiFunction<IValue, IValue, ITuple> constructTuple) {
        super();

        this.constructTuple = constructTuple;

        elementTypeBag = AbstractTypeBag.of();
    }

    private void put(IValue element) {
        final Type elementType = element.getType();

        if (builder == null || elementType != leastUpperBound) {
            if (elementType.isTuple() && elementType.getArity() == 2) {
                if (builder == null) {
                    // first tuple was a binary one, so let's assume all will be binary
                    builder = new MultiMapBuilder();
                }
            }
            else if (builder == null) {
                // first values was not a binary tuple, so let's build a normal set
                builder = new SetBuilder(); 
            }
            else if (builder instanceof MultiMapBuilder) {
                // special case, previous values were all binary tuples, but the new value isn't
                MultiMapBuilder oldBuilder = (MultiMapBuilder) builder;
                builder = new SetBuilder();
                final Builder finalSetBuilder = builder;
                oldBuilder.map.tupleStream(constructTuple).forEach(t -> finalSetBuilder.put(t, t.getType()));        
            }
        }

        builder.put(element, elementType);

        leastUpperBound = leastUpperBound.lub(elementType);
    }

    @Override
    public void insert(IValue... values) throws FactTypeUseException {
        checkMutation();
        Arrays.stream(values).forEach(this::put);
    }

    @Override
    public void insertAll(Iterable<? extends IValue> collection) throws FactTypeUseException {
        checkMutation();
        collection.forEach(this::put);
    }

    @Override
    public ISet done() {
        if (constructedSet != null) {
            return constructedSet;
        }

        if (leastUpperBound == TypeFactory.getInstance().voidType() || builder == null) {
            constructedSet = EmptySet.EMPTY_SET;
            return constructedSet;
        }

        return constructedSet = builder.done();
    }

    // TODO: extract to a utilities class
    @SuppressWarnings("unchecked")
    public static <T, R> Function<T, R> asInstanceOf(Class<R> resultClass) {
        return item -> (R) item;
    }

    // TODO: extract to a utilities class
    public static <T> Predicate<T> isInstanceOf(Class<T> inputClass) {
        return item -> inputClass.isInstance(item);
    }

    private void checkMutation() {
        if (constructedSet != null) {
            throw new UnsupportedOperationException("Mutation of a finalized set is not supported.");
        }
    }

    @Override
    public String toString() {
        if (builder == null) {
            return "{}";
        }

        return builder.toString();
    }

    @Override
    public void insertTuple(IValue... fields) {
        insert(Tuple.newTuple(fields));
    }

    @Override
    public Iterator<IValue> iterator() {
        if (builder == null) {
            return Collections.emptyIterator();
        }

        return builder.iterator();
    }

    @Override
    public Supplier<IWriter<ISet>> supplier() {
        return () -> ValueFactory.getInstance().setWriter();
    }
}
