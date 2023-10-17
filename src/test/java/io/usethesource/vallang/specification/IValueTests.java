package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;
import java.util.PrimitiveIterator.OfInt;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.io.StandardTextReader;
import io.usethesource.vallang.io.StandardTextWriter;
import io.usethesource.vallang.type.TypeStore;

public class IValueTests {
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class) 
    public void equalsIsReflexive(IValue val) {
        assertEquals(val, val);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)  
    public void equalsIsCommutative(IValue val1, IValue val2) {
        assertTrue(!val1.equals(val2) || val2.equals(val1));
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class) 
    public void equalsIsTransitive(IValue val1, IValue val2, IValue val3) {
        assertTrue(!(val1.equals(val2) && val2.equals(val3)) || val1.equals(val3));
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testHashCodeContract(IValue val1, IValue val2) {
        if (val1.equals(val2)) {
            assertEquals(val1.hashCode(), val2.hashCode(), "" + val1.toString() + " and " + val2.toString() + " are equal but do not have the same hashCode?");
        }
        assertTrue(!val1.equals(val2) || val1.hashCode() == val2.hashCode());
    }
    

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintContract(IValue val1, IValue val2) {
        if (val1.equals(val2)) {
            assertEquals(val1.getMatchFingerprint(), val2.getMatchFingerprint(), "" + val1.toString() + " and " + val2.toString() + " are equal but do not have the same fingerprint?");
        }
        assertTrue(!val1.equals(val2) || val1.getMatchFingerprint() == val2.getMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDefaultFingerprintContracts(IValue val1) {
        assertEquals(IValue.getDefaultMatchFingerprint(), 0);
        assertNotEquals(IValue.getDefaultMatchFingerprint(), val1.getMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityIntegersDoNotChangeTheTest(IValueFactory vf, IInteger integer) {
        assertEquals(integer.equals(vf.integer(0)) ? "int".hashCode() : integer.hashCode(), integer.getMatchFingerprint());

        // this should stay or we have to make sure that the fingerprint works like that again
        // if it changes
        if (!integer.equals(vf.integer(0)) && integer.less(vf.integer(Integer.MAX_VALUE)).getValue() && integer.greater(vf.integer(Integer.MIN_VALUE)).getValue()) {
            // copied the implementation of IntegerValue.hashCode here
            // because this is now officially a contract.
            int hash = integer.intValue() ^ 0x85ebca6b;
		    hash ^= hash >>> 13;
		    hash *= 0x5bd1e995;
		    hash ^= hash >>> 15;

            assertEquals(hash, integer.getMatchFingerprint());
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityStringDoNotChangeTheTest(IString string) {
        assertEquals(string.length() == 0 ? "str".hashCode() : string.hashCode(), string.getMatchFingerprint());  

        // we copied the generic hashCode implementation here, to check the contract.
        int h = 0;
        OfInt it = string.iterator();

        while (it.hasNext()) {
            int c = it.nextInt();

            if (!Character.isBmpCodePoint(c)) {
                h = 31 * h + Character.highSurrogate(c);
                h = 31 * h + Character.lowSurrogate(c);
            } else {
                h = 31 * h + ((char) c);
            }
        }

        if (string.length() != 0) {
            assertEquals(h, string.getMatchFingerprint());
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityRealDoNotChangeTheTest(IReal real) {
        assertEquals(real.hashCode() == 0 ? "real".hashCode() : real.hashCode(), real.getMatchFingerprint());
    }

     @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityRationalDoNotChangeTheTest(IRational rational) {
        assertEquals(rational.hashCode(), rational.getMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityListDoNotChangeTheTest(IList list) {
        assertEquals("list".hashCode(), list.getMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintAllListsTheSameDoNotChangeTheTest(IList list1, IList list2) {
        assertEquals(list1.getMatchFingerprint(), list2.getMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilitySetDoNotChangeTheTest(ISet set) {
        assertEquals("set".hashCode(), set.getMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintAllSetsTheSameDoNotChangeTheTest(ISet set1, ISet set2) {
        assertEquals(set1.getMatchFingerprint(), set2.getMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityMapDoNotChangeTheTest(IMap map) {
        assertEquals("map".hashCode(), map.getMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintAllMapsTheSameDoNotChangeTheTest(IMap map1, IMap map2) {
        assertEquals(map1.getMatchFingerprint(), map2.getMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityTupleDoNotChangeTheTest(ITuple tuple) {
        assertEquals(("tuple".hashCode() << 2) + tuple.arity(), tuple.getMatchFingerprint());        
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintEqualArityTuplesTheSameDoNotChangeTheTest(ITuple tuple1, ITuple tuple2) {
        if (tuple1.arity() == tuple2.arity()) {
            assertEquals(tuple1.getMatchFingerprint(), tuple2.getMatchFingerprint());
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityNodeDoNotChangeTheTest(ISourceLocation node) {       
        assertEquals(node.hashCode(), node.getMatchFingerprint());     
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityNodeDoNotChangeTheTest(INode node) {       
        assertEquals(node.getName().hashCode() == 0 
            ?  ("node".hashCode() << 2) + node.arity() 
            : node.getName().hashCode() + 131 * node.arity(), node.getMatchFingerprint());     
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintEqualArityNodesTheSameDoNotChangeTheTest(INode node1, INode node2) {
        if (node1.arity() == node2.arity() && node1.getName().equals(node2.getName())) {
            assertEquals(node1.getMatchFingerprint(), node2.getMatchFingerprint());
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityNodesMatchConstructorsDoNotChangeTheTest(IValueFactory vf, IConstructor constructor) {       
        assertEquals(
            constructor.getMatchFingerprint(),
            vf.node(constructor.getName(), StreamSupport.stream(constructor.getChildren().spliterator(), false).toArray(IValue[]::new)).getMatchFingerprint()
        );
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStabilityConstructorDoNotChangeTheTest(IConstructor constructor) {
        assertEquals(constructor.getName().hashCode() + 131 * constructor.arity(), constructor.getMatchFingerprint());        
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintEqualArityConstructorsTheSameDoNotChangeTheTest(IConstructor node1, IConstructor node2) {
        if (node1.arity() == node2.arity()) {
            assertEquals(node1.getMatchFingerprint(), node2.getMatchFingerprint());
        }
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class) 
    public void testWysiwyg(IValueFactory vf, TypeStore store, IValue val) throws FactTypeUseException, IOException {
        StandardTextReader reader = new StandardTextReader();
        String string = val.toString();
        IValue result = reader.read(vf, store, val.getType(), new StringReader(string));
        assertEquals(val, result, "reading back " + val + " produced something different");
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class) 
    public void testWysiwygConstructors(IValueFactory vf, TypeStore store, IConstructor val) throws FactTypeUseException, IOException {
        // constructors go wrong more often, this test makes sure we generate more random examples.
        testWysiwyg(vf, store, val);
    }

    public void testIsomorphicText(IValue val1, IValue val2) throws FactTypeUseException, IOException {
        // (val1 == val2) <==> (val1.toString() == val2.toString())

        if (val1.equals(val2)) {
            assertEquals(val1.toString(), val2.toString(), val1.toString() + " and " + val2.toString() + " should look the same because they are equal.");
        }
        
        if (val1.toString().equals(val2.toString())) {
            assertEquals(val1, val2, val1.toString() + " and " + val2.toString() + "should be equal because they look the same.");
        }
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testToStringIsStandardardTextWriter(IValueFactory vf, IValue val) throws FactTypeUseException, IOException {
        assertEquals(val.toString(), StandardTextWriter.valueToString(val), "toString of " + val + " is not equal to the standard notation");
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testNoValueInstancesShouldEverHaveFieldNamesInTheirDynamicTypes(IValue val) {
        assertFalse(val.getType().hasFieldNames());
    }
}
