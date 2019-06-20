package io.usethesource.vallang.specification;

import java.util.Collections;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.impl.reference.ValueFactory;
import io.usethesource.vallang.random.RandomTypeGenerator;
import io.usethesource.vallang.random.RandomValueGenerator;
import io.usethesource.vallang.type.TypeStore;

public class AbstractSpecification {
    protected static final IValueFactory vf = ValueFactory.getInstance(); // TODO: multiple factory support for these tests...
    protected static final TypeStore store = new TypeStore();
    protected static final RandomValueGenerator gen = new RandomValueGenerator(vf, new Random(), 5, 10, true);
    protected static final RandomValueGenerator genNoAnnos = new RandomValueGenerator(vf, new Random(), 5, 10, false);
    protected static final RandomTypeGenerator typeGen = new RandomTypeGenerator();
    protected static final int MAX = 100;

    private static IValue nextValue() {
        return gen.generate(typeGen.next(10), store, Collections.emptyMap());
    }
     
    private static IValue nextValueNoAnnos() {
        return genNoAnnos.generate(typeGen.next(10), store, Collections.emptyMap());
    }
    
    protected static Stream<Arguments> anyValue() {
        return Stream.iterate(Arguments.of(nextValue()), i -> Arguments.of(nextValue())).limit(MAX);
    }
    
    protected static Stream<Arguments> anyValueNoAnnos() {
        return Stream.iterate(Arguments.of(nextValueNoAnnos()), i -> Arguments.of(nextValueNoAnnos())).limit(MAX);
    }
    
    protected static Stream<Arguments> anyTwoValues() {
        return Stream.iterate(Arguments.of(nextValue(), nextValue()), i -> Arguments.of(nextValue(), nextValue())).limit(MAX);
    }
    
    protected static Stream<Arguments> anyThreeValues() {
        return Stream.iterate(Arguments.of(nextValue(), nextValue(), nextValue()), i -> Arguments.of(nextValue(), nextValue(), nextValue())).limit(MAX);
    }
}