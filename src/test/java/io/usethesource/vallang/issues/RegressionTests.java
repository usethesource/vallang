package io.usethesource.vallang.issues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.io.StandardTextWriter;
import java.io.IOException;
import java.io.StringWriter;
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
        
        // To trigger the bug both sets had to be of equal arity,
        // to avoid short-circuiting the equality check on that.
        
        if (!rel.isEmpty() && !set.isEmpty()) {
            assertTrue(!rel.equals(set));
            assertTrue(!set.equals(rel));
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    void toStringSourceLocationEqualsTextWriter(IValueFactory vf, ISourceLocation loc) throws IOException {
        StandardTextWriter writer = new StandardTextWriter(false);
        StringWriter target = new StringWriter();
        writer.write(loc, target);
        assertEquals(target.toString(), loc.toString());
    }
}
