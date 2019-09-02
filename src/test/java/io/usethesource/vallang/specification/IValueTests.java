package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.io.StandardTextReader;
import io.usethesource.vallang.io.StandardTextWriter;

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
    public void testWysiwyg(IValueFactory vf, IValue val) throws FactTypeUseException, IOException {
        StandardTextReader reader = new StandardTextReader();
        String string = val.toString();
        IValue result = reader.read(vf, val.getType(), new StringReader(string));
        assertEquals(val, result, "reading back " + val + " produced something different");
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
    public void testWysiwygAnnos(IValueFactory vf, IValue val) throws FactTypeUseException, IOException {
        StandardTextReader reader = new StandardTextReader();
        String string = val.toString();
        IValue result = reader.read(vf, val.getType(), new StringReader(string));
        assertTrue(val.equals(result), val.toString() + " is not read back properly."); // isEqual ignores annotations
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
