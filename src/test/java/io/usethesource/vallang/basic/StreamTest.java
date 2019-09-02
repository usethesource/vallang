package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.GivenValue;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.visitors.ValueStreams;

public class StreamTest {
    private static final String TREE = "\"1\"(\"2\"(\"4\"(),\"5\"()), \"3\"(\"6\"(),\"7\"()))";
    private static final String BOTTOMUP = "[\"4\",\"5\",\"2\",\"6\",\"7\",\"3\",\"1\"]";
    private static final String TOPDOWN = "[\"1\",\"2\",\"4\",\"5\",\"3\",\"6\",\"7\"]";
    private static final String TOPDOWNBREADTHFIRST = "[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\"]";
    private static final String BOTTOMUPBREADTHFIRST = "[\"7\",\"6\",\"5\",\"4\",\"3\",\"2\",\"1\"]";

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void bottomup(IValueFactory vf, @GivenValue(TREE) IValue v, @GivenValue(BOTTOMUP) IValue answer) {
        IList numbers = ValueStreams.bottomup(v).map((n) -> vf.string(((INode) n).getName())).collect(vf.listWriter());

        assertEquals(answer, numbers);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void topdown(IValueFactory vf, @GivenValue(TREE) IValue v, @GivenValue(TOPDOWN) IValue answer) {
        IList numbers = ValueStreams.topdown(v).map((n) -> vf.string(((INode) n).getName())).collect(vf.listWriter());

        assertEquals(answer, numbers);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void topdownBreadthFirst(IValueFactory vf, @GivenValue(TREE) IValue v, @GivenValue(TOPDOWNBREADTHFIRST) IValue answer) {
        IList numbers = ValueStreams.topdownbf(v).map((n) -> vf.string(((INode) n).getName())).collect(vf.listWriter());

        assertEquals(answer, numbers);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void bottomupBreadthFirst(IValueFactory vf, @GivenValue(TREE) IValue v, @GivenValue(BOTTOMUPBREADTHFIRST) IValue answer) {
        IList numbers = ValueStreams.bottomupbf(v).map((n) -> vf.string(((INode) n).getName())).collect(vf.listWriter());

        assertEquals(answer, numbers);
    }
}
