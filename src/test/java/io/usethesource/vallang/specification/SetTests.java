package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;

public class SetTests {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void idempotence( @ExpectedType("set[int]") ISet set, IInteger i) {
        assertEquals(set.insert(i), set.insert(i).insert(i));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void idempotenceWithLoc( @ExpectedType("set[loc]") ISet set, ISourceLocation i) {
        assertEquals(set.insert(i), set.insert(i).insert(i));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void insertion( @ExpectedType("set[real]") ISet set, IInteger i) {
        assertEquals(set.insert(i).size(), set.size() + 1);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void removal(Random rnd, IValueFactory vf, @ExpectedType("set[int]") ISet set) {
        if (set.isEmpty()) {
            return;
        }
        int index = rnd.nextInt(set.size());
        IValue elem = set.stream().skip(index).findFirst().get();
        ISet smaller = set.delete(elem);
        assertEquals(smaller.size() + 1, set.size());
        assertTrue(!smaller.contains(elem));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void removalLocations(Random rnd, @ExpectedType("set[loc]") ISet set, ISourceLocation i) {
        if (set.isEmpty()) {
            return;
        }

        int index = rnd.nextInt(set.size());
        IValue elem = set.stream().skip(index).findFirst().get();
        ISet smaller = set.delete(elem);
        assertEquals(smaller.size() + 1, set.size());
        assertTrue(!smaller.contains(elem));
    }
}
