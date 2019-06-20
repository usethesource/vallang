package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.io.StandardTextReader;

public class IValueSpecification extends AbstractSpecification {
    
    @ParameterizedTest(name="equalsIsReflexive({0})")
    @MethodSource("anyValue")
    public void equalsIsReflexive(IValue val) {
        assertEquals(val, val);
    }
    
    @ParameterizedTest(name="equalsIsCommutative({0},{1})")
    @MethodSource("anyTwoValues")
    public void equalsIsCommutative(IValue val1, IValue val2) {
        assertTrue(!val1.equals(val2) || val2.equals(val1));
    }
    
    @ParameterizedTest(name="equalsIsTransitive({0},{1},{2})")
    @MethodSource("anyThreeValues")
    public void equalsIsTransitive(IValue val1, IValue val2, IValue val3) {
        assertTrue(!(val1.equals(val2) && val2.equals(val3)) || val1.equals(val3));
    }
    
    @ParameterizedTest(name="hashCodeContract({0},{1})")
    @MethodSource("anyTwoValues")
    public void testHashCodeContract(IValue val1, IValue val2) {
        assertTrue(!val1.equals(val2) || val1.hashCode() == val2.hashCode());
    }
    
    @ParameterizedTest(name="wysiwyg({0})")
    @MethodSource("anyValueNoAnnos")
    public void testWysiwyg(IValue val) throws FactTypeUseException, IOException {
        StandardTextReader reader = new StandardTextReader();
        String string = val.toString();
        IValue result = reader.read(vf, val.getType(), new StringReader(string));
        assertEquals(val, result, () -> val.toString() + " != " + result.toString());
    }
    
    @ParameterizedTest(name="testIsomorphicText({0},{1})")
    @MethodSource("anyTwoValues")
    public void testIsomorphicText(IValue val1, IValue val2) throws FactTypeUseException, IOException {
        // (val1 == val2) <==> (val1.toString() == val2.toString())
        assertTrue(!val1.equals(val2) || val1.toString().equals(val2.toString()));
        assertTrue(!val1.toString().equals(val2.toString()) || val1.equals(val2));
    }
    
    @ParameterizedTest(name="wysiwygAnnos({0})")
    @MethodSource("anyValue")
    public void testWysiwygAnnos(IValue val) throws FactTypeUseException, IOException {
        StandardTextReader reader = new StandardTextReader();
        String string = val.toString();
        IValue result = reader.read(vf, val.getType(), new StringReader(string));
        assertTrue(val.isEqual(result)); // isEqual ignores annotations
    }
}
