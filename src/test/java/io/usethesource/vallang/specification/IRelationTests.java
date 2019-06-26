package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.GivenValue;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;

public class IRelationTests {

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void listRelationProjectOrder(IValueFactory vf, @ExpectedType("lrel[int,int]") IList l) {
        Iterator<IValue> original = l.iterator();
        Iterator<IValue> projected = l.asRelation().project(1,0).iterator();
        
        while (original.hasNext()) {
            if (!projected.hasNext()) {
                fail("projected list should be equal length");
            }
            
            ITuple one = (ITuple) original.next();
            ITuple two = (ITuple) projected.next();
            
            assertEquals(one.select(1,0), two, "elements should appear in original order");
        }
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void transReflexiveClosure(
        @GivenValue("{<1,2>, <2,3>, <3,4>}") ISet src, 
        @GivenValue("{<1,2>, <2,3>, <3,4>, <1, 3>, <2, 4>, <1, 4>, <1, 1>, <2, 2>, <3, 3>, <4, 4>}") ISet result) {
        assertEquals(src.asRelation().closureStar(), result);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void transClosure(@ExpectedType("rel[int,int]") ISet src) {
        assertEquals(src.asRelation().closure().intersect(src), src);
    }
}
