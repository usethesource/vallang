package io.usethesource.vallang.issues;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.GivenValue;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;

public class RegressionTests {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void iTupleCastExceptionsInEquals(IValueFactory vf, 
        @GivenValue("{<5,0>,<1330107671,7>,<0,0>}") ISet rel,
        @GivenValue("{6,-1426731573,0}") ISet set) {
        // these calls would throw ClassCastExceptions because the 
        // receiver or the argument was specialized as a binary relation
        // and contained tuples, while the other was not. Still a cast to
        // ITuple was performed.
        
        if (!rel.isEmpty() && !set.isEmpty()) {
            assertTrue(!rel.equals(set));
            assertTrue(!set.equals(rel));
            assertTrue(!rel.isEqual(set));
            assertTrue(!set.isEqual(rel));
        }
    }
}
