package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

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
            assertEquals(val1.getPatternMatchFingerprint(), val2.getPatternMatchFingerprint(), "" + val1.toString() + " and " + val2.toString() + " are equal but do not have the same fingerprint?");
        }
        assertTrue(!val1.equals(val2) || val1.getPatternMatchFingerprint() == val2.getPatternMatchFingerprint());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testFingerprintStability(IInteger integer, IString string, IReal real, IRational rational, IList list, ISet set, IMap map, ITuple tuple, IConstructor constructor, INode node) {
        // if we really want to change these codes, we should be aware that we are breaking all previously compiled and released Rascal code.

        assertEquals(integer.hashCode(), integer.getPatternMatchFingerprint());
        assertEquals(string.hashCode(), string.getPatternMatchFingerprint());
        assertEquals(real.hashCode(), real.getPatternMatchFingerprint());
        assertEquals(rational.hashCode(), rational.getPatternMatchFingerprint());
        assertEquals("list".hashCode(), list.getPatternMatchFingerprint());
        assertEquals("set".hashCode(), set.getPatternMatchFingerprint());
        assertEquals("map".hashCode(), map.getPatternMatchFingerprint());
        assertEquals("tuple".hashCode() << 2 + tuple.arity(), tuple.getPatternMatchFingerprint());        
        assertEquals(constructor.getName().hashCode() << 2 + constructor.arity(), constructor.getPatternMatchFingerprint());        
        assertEquals(node.getName().hashCode() << 2 + node.arity(), node.getPatternMatchFingerprint());        
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
