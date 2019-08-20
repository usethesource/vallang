package io.usethesource.vallang.impl.persistent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.IWriter;
import io.usethesource.vallang.exceptions.IllegalOperationException;
import io.usethesource.vallang.impl.util.collections.ShareableValuesHashSet;
import io.usethesource.vallang.impl.util.collections.ShareableValuesList;
import io.usethesource.vallang.util.RotatingQueue;
import io.usethesource.vallang.util.ValueEqualsWrapper;
import io.usethesource.vallang.util.ValueIndexedHashMap;

public class PersistentSetRelation implements IRelation<ISet> {
    private final ISet set;

    public PersistentSetRelation(ISet set) {
        this.set = set;
    }
    
    @Override
    public ISet asContainer() {
        return set;
    }

    @Override
    public ISet compose(IRelation<ISet> set2) {
        if (this.getElementType().isBottom()) {
            return set;
        }
        
        if (set2.getElementType().isBottom()) {
            return set2.asContainer();
        }

        if (this.getElementType().getArity() != 2
                || set2.getElementType().getArity() != 2) {
            throw new IllegalOperationException(
                    "Incompatible types for composition.",
                    this.getElementType(), set2.getElementType());
        }
        
        if (!this.getElementType().getFieldType(1)
                .comparable(set2.getElementType().getFieldType(0))) {
            return EmptySet.of();
        }
        
        // TODO: recomputing this index is weird, since it can be expected a
        // relation is already indexed.
        
        // Index
        Map<ValueEqualsWrapper, ShareableValuesList> rightSides = new HashMap<>();
        
        for (IValue val : set2) {
            ITuple tuple = (ITuple) val;
            ValueEqualsWrapper key = new ValueEqualsWrapper(tuple.get(0));
            ShareableValuesList values = rightSides.get(key);
            if (values == null) {
                values = new ShareableValuesList();
                rightSides.put(key, values);
            }
            
            values.append(tuple.get(1));
        }

        // build result using index
        
        IWriter<ISet> resultWriter = writer();
        for (IValue thisVal : this) {
            ITuple thisTuple = (ITuple) thisVal;
            ValueEqualsWrapper key = new ValueEqualsWrapper(thisTuple.get(1));
            ShareableValuesList values = rightSides.get(key);
            if (values != null){
                Iterator<IValue> valuesIterator = values.iterator();
                do {
                    IValue value = valuesIterator.next();
                    resultWriter.insertTuple(thisTuple.get(0), value);
                } while(valuesIterator.hasNext());
            }
        }
        
        return resultWriter.done();
    }
    
    @Override
    public ISet closure() {
        IWriter<ISet> resultWriter = writer();
        resultWriter.insertAll(this);
        resultWriter.insertAll(computeClosureDelta());
        return resultWriter.done();
    }

    
    @Override
    public ISet closureStar() {
        // calculate
        Set<IValue> closureDelta = computeClosureDelta();

        IWriter<ISet> resultWriter = writer();
        resultWriter.insertAll(this);
        resultWriter.insertAll(closureDelta);
        
        for (IValue element : carrier()) {
            resultWriter.insertTuple(element, element);
        }

        return resultWriter.done();
    }
    
    private Set<IValue> computeClosureDelta() {
        IValueFactory vf = ValueFactory.getInstance();
        RotatingQueue<ValueEqualsWrapper> iLeftKeys = new RotatingQueue<>();
        RotatingQueue<RotatingQueue<ValueEqualsWrapper>> iLefts = new RotatingQueue<>();
        
        Map<ValueEqualsWrapper, RotatingQueue<ValueEqualsWrapper>> interestingLeftSides = new HashMap<>();
        Map<ValueEqualsWrapper, Set<ValueEqualsWrapper>> potentialRightSides = new HashMap<>();
        
        // Index
        for (IValue val : this) {
            ITuple tuple = (ITuple) val;
            ValueEqualsWrapper key = new ValueEqualsWrapper(tuple.get(0));
            ValueEqualsWrapper value = new ValueEqualsWrapper(tuple.get(1));
            RotatingQueue<ValueEqualsWrapper> leftValues = interestingLeftSides.get(key);
            Set<ValueEqualsWrapper> rightValues;
            
            if (leftValues != null) {
                rightValues = potentialRightSides.get(key);
            } else {
                leftValues = new RotatingQueue<>();
                iLeftKeys.put(key);
                iLefts.put(leftValues);
                interestingLeftSides.put(key, leftValues);
                
                rightValues = new HashSet<>();
                potentialRightSides.put(key, rightValues);
            }
            
            leftValues.put(value);
            if (rightValues == null) {
                rightValues = new HashSet<>();
            }
            
            rightValues.add(value);
        }
        
        int size = potentialRightSides.size();
        int nextSize = 0;
        
        // Compute
        final Set<IValue> newTuples = new HashSet<>();
        do{
            Map<ValueEqualsWrapper, Set<ValueEqualsWrapper>> rightSides = potentialRightSides;
            potentialRightSides = new HashMap<>();
            
            for(; size > 0; size--){
                ValueEqualsWrapper leftKey = iLeftKeys.get();
                RotatingQueue<ValueEqualsWrapper> leftValues = iLefts.get();
                
                RotatingQueue<ValueEqualsWrapper> interestingLeftValues = null;
                
                ValueEqualsWrapper rightKey;
                while((rightKey =  leftValues.get()) != null){
                    Set<ValueEqualsWrapper> rightValues = rightSides.get(rightKey);
                    if(rightValues != null){
                        Iterator<ValueEqualsWrapper> rightValuesIterator = rightValues.iterator();
                        while(rightValuesIterator.hasNext()){
                            ValueEqualsWrapper rightValue = rightValuesIterator.next();
                            if(newTuples.add(vf.tuple(leftKey.getValue(), rightValue.getValue()))){
                                if(interestingLeftValues == null){
                                    nextSize++;
                                    
                                    iLeftKeys.put(leftKey);
                                    interestingLeftValues = new RotatingQueue<>();
                                    iLefts.put(interestingLeftValues);
                                }
                                interestingLeftValues.put(rightValue);
                                
                                Set<ValueEqualsWrapper> potentialRightValues = potentialRightSides.get(rightKey);
                                if(potentialRightValues == null){
                                    potentialRightValues = new HashSet<>();
                                    potentialRightSides.put(rightKey, potentialRightValues);
                                }
                                potentialRightValues.add(rightValue);
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
