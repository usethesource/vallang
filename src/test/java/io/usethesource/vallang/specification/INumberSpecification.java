package io.usethesource.vallang.specification;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.INumber;
import io.usethesource.vallang.IValueFactory;

public class INumberSpecification {
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void divByZero(IValueFactory vf, INumber a) {
        Assertions.assertThrows(ArithmeticException.class, () -> a.divide(vf.integer(0), 10));
    }
}
