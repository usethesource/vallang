package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map.Entry;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public class IMapTests {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void canonicalEmptyMap(IValueFactory vf, @ExpectedType("map[value,value]") IMap m) {
        for (IValue key : m) {
            m = m.removeKey(key);
        }
        
        assertTrue(m.isEmpty());
        assertTrue(m.equals(vf.map()));
    }
    
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void lubInvariant(TypeFactory tf, @ExpectedType("map[value,value]") IMap m) {
        Type keyLub = tf.voidType();
        Type valueLub = tf.voidType();
        
        for (Entry<IValue, IValue> entry : (Iterable<Entry<IValue,IValue>>) () -> m.entryIterator()) {
            keyLub = keyLub.lub(entry.getKey().getType());
            valueLub = valueLub.lub(entry.getValue().getType());
        }

        assertEquals(tf.mapType(keyLub, valueLub), m.getType());
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void lubInvariantAfterRemoveKey(TypeFactory tf, @ExpectedType("map[int,int]") IMap m) {
        for (IValue key : m) {
            m = m.removeKey(key);
            lubInvariant(tf, m);
        }
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void mapIsVoidAfterAllRemoved(TypeFactory tf, IMap m) {
        IMap copy = m;
        for (IValue key : m) {
            copy = copy.removeKey(key);
        }
        
        // this failed due to issue #55 but only if the random generator
        // accidentally adds two of the same key/value pairs to the map
        assertTrue(copy.getKeyType() == tf.voidType());
        assertTrue(copy.getValueType() == tf.voidType());
    }
 }
