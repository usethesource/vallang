package io.usethesource.vallang.issues;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.GivenValue;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.io.StandardTextReader;
import io.usethesource.vallang.io.StandardTextWriter;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

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

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    void keywordFieldsMakeConstructorsDifferent(IValueFactory vf, TypeFactory tf, TypeStore store)  {
        Type X = tf.abstractDataType(store, "X");
        Type cons = tf.constructor(store, X, "x");
        store.declareKeywordParameter(X, "name", tf.stringType());

        IConstructor cons1 = vf.constructor(cons).asWithKeywordParameters().setParameter("name", vf.string("paul"));
        IConstructor cons2 = vf.constructor(cons).asWithKeywordParameters().setParameter("name", vf.string("jurgen"));

        assertFalse(cons1.equals(cons2));
    }

    private IString readString(IValueFactory valueFactory, TypeStore typeStore, String s) throws IOException {
        Reader reader = new StringReader(s);
        StandardTextReader textReader = new StandardTextReader();
        return (IString) textReader.read(valueFactory, typeStore, TypeFactory.getInstance().stringType(), reader);

    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    void escapeNormalCharacters(IValueFactory valueFactory, TypeStore typeStore) throws IOException {
        IString s = readString(valueFactory, typeStore, "\"\\$\"");
        assertEquals("$", s.getValue());
    }

}
