package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;

public class StreamCollectorTest {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void listCollector(IValueFactory vf, @ExpectedType("list[int]") IList elems) {
        IList l = elems.stream().collect(vf.listWriter());
        assertEquals(l, elems);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void setCollector(IValueFactory vf, @ExpectedType("set[int]") ISet elems) {
        ISet l = elems.stream().collect(vf.setWriter());
        assertEquals(l, elems);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void mapCollector(IValueFactory vf, @ExpectedType("map[int,int]") IMap elems) {
        IMap l = elems.stream().collect(vf.mapWriter());
        assertEquals(l, elems);
    }
}
