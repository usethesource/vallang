package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.GivenValue;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.ValueProvider;

public class ListTests {
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void intersectionOrder(
            @GivenValue("[1,2,3,4,5]") IList l1, 
            @GivenValue("[5,3,1]") IList l2,
            @GivenValue("[1,3,5]") IList result
            ) {
        assertEquals(l1.intersect(l2), result);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void concatLength(@ExpectedType("list[num]") IList l1, @ExpectedType("list[num]") IList l2) {
        assertEquals(l1.concat(l2).length(), l1.length() + l2.length());
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void iterationOrder(@ExpectedType("list[num]") IList l1) {
        int i = 0;
        for (IValue elem : l1) {
            assertEquals(elem, l1.get(i++));
        }
    }
}
