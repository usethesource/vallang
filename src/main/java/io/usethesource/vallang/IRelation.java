package io.usethesource.vallang;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.impl.util.collections.ShareableValuesHashSet;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.util.RotatingQueue;
import io.usethesource.vallang.util.ValueIndexedHashMap;

public interface IRelation<C extends ICollection<C>> extends Iterable<ITuple> {	

    @Override
    default Iterator<ITuple> iterator() {
        return new Iterator<ITuple>() {
            Iterator<IValue> it = asContainer().iterator();
            
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public ITuple next() {
                return (ITuple) it.next();
            }
        };
    }
    
    default C compose(IRelation<C> that) {
        C thisContainer = this.asContainer();
        C thatContainer = that.asContainer();
        Type thisElementType = thisContainer.getElementType();
        Type thatElementType = thatContainer.getElementType();

        if (thisElementType.isBottom()) {
            return thisContainer;
        }

        if (thatElementType.isBottom()) {
            return thatContainer;
        }

        if (thisElementType.getArity() != 2 || thatElementType.getArity() != 2) {
            throw new IllegalOperationException("Incompatible types for composition.", thisElementType, thatElementType);
        }

        if (!thisElementType.getFieldType(1).comparable(thatElementType.getFieldType(0))) {
            return asContainer().empty();
        }

        // Index
        Map<IValue, IWriter<C>> rightSides = new HashMap<>();

        for (ITuple tuple : that) {    
            IValue key = tuple.get(0);

            IWriter<C> values = rightSides.get(key);
            if(values == null){
                values = writer();
                rightSides.put(key, values);
            }

            values.append(tuple.get(1));
        }

        // Compute      
        IWriter<C> resultWriter = thisContainer.writer();

        for (ITuple thisTuple : this) {
            IValue key = thisTuple.get(1);
            IWriter<C> values = rightSides.get(key);
            
            if (values != null) {
                for (IValue value : values) {
                    resultWriter.appendTuple(thisTuple.get(0), value);
                }
            }
        }

        return resultWriter.done();
    }   

    default IRelation<C> closure() {
        C rel1 = asContainer();

        if (rel1.getElementType().isBottom()) {
            return this;
        }

        if (!isBinary()) {
            throw new IllegalOperationException("closure", rel1.getType());
        }

        Type tupleElementType = rel1.getElementType().getFieldType(0).lub(rel1.getElementType().getFieldType(1));
        Type tupleType = TypeFactory.getInstance().tupleType(tupleElementType, tupleElementType);

        java.util.Set<IValue> closureDelta = HelperFunctions.computeClosureDelta(this, tupleType);

        // NOTE: type is already known, thus, using a SetWriter degrades performance
        IWriter<C> resultWriter = rel1.writer();
        resultWriter.insertAll(rel1);
        resultWriter.insertAll(closureDelta);

        return resultWriter.done().asRelation();
    }


    default IRelation<C> closureStar() {
        if (getElementType().isBottom()) {
            return this;
        }

        if (!isBinary()) {
            throw new IllegalOperationException("closureStar", asContainer().getType());
        }

        Type tupleElementType = getElementType().getFieldType(0).lub(getElementType().getFieldType(1));
        Type tupleType = TypeFactory.getInstance().tupleType(tupleElementType, tupleElementType);

        // calculate
        ShareableValuesHashSet closureDelta = HelperFunctions.computeClosureDelta(this, tupleType);
        C carrier = carrier();

        // aggregate result
        // NOTE: type is already known, thus, using a SetWriter degrades performance
        IWriter<C> resultWriter = writer();
        resultWriter.insertAll(this);
        resultWriter.insertAll(closureDelta);

        Iterator<IValue> carrierIterator = carrier.iterator();
        while (carrierIterator.hasNext()) {
            IValue element = carrierIterator.next();
            resultWriter.insertTuple(element, element);
        }

        return resultWriter.done().asRelation();
    }

    default int arity() {
        return asContainer().getElementType().getArity();
    }

    default C empty() {
        return asContainer().empty();
    }
    
    default C project(int... fields) {
        IWriter<C> w = writer();

        for (ITuple v : this) {
            w.insert(v.select(fields));
        }

        return w.done();
    }

    @Deprecated
    default C projectByFieldNames(String... fields) {
        C collection = asContainer();
        int[] indexes = new int[fields.length];
        int i = 0;

        if (!collection.getType().getFieldTypes().hasFieldNames()) {
            throw new IllegalOperationException("project with field names", collection.getType());
        }

        for (String field : fields) {
            indexes[i++] = collection.getType().getFieldTypes().getFieldIndex(field);
        }

        return project(indexes);
    }

    default C carrier() {
        IWriter<C> w = writer();

        for (ITuple t : this) {
            w.insertAll(t);
        }

        return w.done();
    }

    default C domain() {
        IWriter<C> w = asContainer().writer();

        for (ITuple elem : this) {
            w.insert(elem.get(0));
        }

        return w.done();
    }

    default C range() {
        int columnIndex = arity() - 1;
        IWriter<C> w = writer();

        for (ITuple elem : this) {
            w.insert(elem.get(columnIndex));
        }

        return w.done();
    }

    default C index(IValue key) {
        C set1 = asContainer();
        Type elementType = getElementType();

        if (elementType.isBottom()) {
            return set1.empty();
        }

        int valueArity = elementType.getArity() - 1;

        Function<ITuple, IValue> mapper;
        if (valueArity == 0) {
            mapper = t -> t.get(1);
        }
        else {
            int[] newTupleIndex = new int[valueArity];
            for (int k = 1; k <= valueArity; k++) {
                newTupleIndex[k - 1] =  k;
            }
            mapper = t -> t.select(newTupleIndex);
        }

        IWriter<C> result = writer();
        for (ITuple tup : this) {
            if (tup.get(0).isEqual(key)) {
                result.insert(mapper.apply(tup));
            }
        }

        return result.done();
    }

    C asContainer();
    
    default IWriter<C> writer() {
        return asContainer().writer();
    }
    
    default Type getElementType() {
        return asContainer().getElementType();
    }

    default boolean isBinary() {
        return getElementType().getArity() == 2;
    }

    static class HelperFunctions {
        private static <C extends ICollection<C>> ShareableValuesHashSet computeClosureDelta(IRelation<C> x1, Type tupleType) {
            RotatingQueue<IValue> iLeftKeys = new RotatingQueue<>();
            RotatingQueue<RotatingQueue<IValue>> iLefts = new RotatingQueue<>();

            ValueIndexedHashMap<RotatingQueue<IValue>> interestingLeftSides = new ValueIndexedHashMap<>();
            ValueIndexedHashMap<IWriter<C>> potentialRightSides = new ValueIndexedHashMap<>();

            // Index
            for (ITuple tuple : x1) {
                IValue key = tuple.get(0);
                IValue value = tuple.get(1);
                RotatingQueue<IValue> leftValues = interestingLeftSides.get(key);
                IWriter<C> rightValues;
                
                if (leftValues != null) {
                    rightValues = potentialRightSides.get(key);
                } else {
                    leftValues = new RotatingQueue<>();
                    iLeftKeys.put(key);
                    iLefts.put(leftValues);
                    interestingLeftSides.put(key, leftValues);

                    rightValues = x1.writer();
                    potentialRightSides.put(key, rightValues);
                }
                
                leftValues.put(value);
                rightValues.append(value);
            }

            int size = potentialRightSides.size();
            int nextSize = 0;

            // Compute
            final ShareableValuesHashSet newTuples = new ShareableValuesHashSet();
            do{
                ValueIndexedHashMap<IWriter<C>> rightSides = potentialRightSides;
                potentialRightSides = new ValueIndexedHashMap<>();

                for (; size > 0; size--){
                    IValue leftKey = iLeftKeys.get();
                    RotatingQueue<IValue> leftValues = iLefts.get();

                    RotatingQueue<IValue> interestingLeftValues = null;

                    IValue rightKey;
                    while ((rightKey = leftValues.get()) != null) {
                        IWriter<C> rightValues = rightSides.get(rightKey);
                        if (rightValues != null) {
                            
                            for (IValue rightValue : rightValues) {
                                if (newTuples.addTuple(leftKey, rightValue)) {
                                    if (interestingLeftValues == null) {
                                        nextSize++;

                                        iLeftKeys.put(leftKey);
                                        interestingLeftValues = new RotatingQueue<>();
                                        iLefts.put(interestingLeftValues);
                                    }
                                    interestingLeftValues.put(rightValue);

                                    IWriter<C> potentialRightValues = potentialRightSides.get(rightKey);
                                    if (potentialRightValues == null) {
                                        potentialRightValues = x1.writer();
                                        potentialRightSides.put(rightKey, potentialRightValues);
                                    }
                                    potentialRightValues.append(rightValue);
                                }
                            }
                        }
                    }
                }
                size = nextSize;
                nextSize = 0;
            } while(size > 0);

            return newTuples;
        }
    }
}
