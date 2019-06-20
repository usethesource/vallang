package io.usethesource.vallang.specification;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.usethesource.vallang.INumber;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.type.TypeFactory;

public class INumberSpecification extends AbstractSpecification {
    private static IValue nextNumber() {
        return gen.generate(TypeFactory.getInstance().numberType(), store, Collections.emptyMap());
    } 
    
    protected static Stream<Arguments> anyNumber() {
        return Stream.iterate(Arguments.of(nextNumber()), i -> Arguments.of(nextNumber())).limit(MAX);
    }
    
    @ParameterizedTest(name="divByZero({0})")
    @MethodSource("anyNumber")
    public void divByZero(INumber a) {
        Assertions.assertThrows(ArithmeticException.class, () -> a.divide(vf.integer(0), 10));
    }
}
