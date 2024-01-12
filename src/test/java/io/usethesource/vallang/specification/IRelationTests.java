package io.usethesource.vallang.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.ExpectedType;
import io.usethesource.vallang.GivenValue;
import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
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
        @GivenValue("{<1,2>, <2,3>, <3,4>, <1, 3>, <2, 4>, <1, 4>, <1, 1>, <2, 2>, <3, 3>, <4, 4>}") ISet result, IBool forceDepthFirst) {
        assertEquals(src.asRelation().closureStar(forceDepthFirst.getValue()), result);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void transClosure(@ExpectedType("rel[int,int]") ISet src, IBool forceDepthFirst) {
        assertEquals(src.asRelation().closure(forceDepthFirst.getValue()).intersect(src), src);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void transClosureLocs(@ExpectedType("rel[loc,loc]") ISet src, IBool forceDepthFirst) {
        assertEquals(src.asRelation().closure(forceDepthFirst.getValue()).intersect(src), src);
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void deepClosure(IValueFactory vf, IBool forceDepthFirst) {
        IValue prev = vf.integer(0);
        var rBuilder = vf.setWriter();
        for (int i=1; i < 100; i++) {
            IValue next = vf.integer(i);
            rBuilder.appendTuple(prev, next);
            prev = next;
        }
        var r = rBuilder.done().asRelation();

        assertEquals(slowClosure(vf, r),r.closure(forceDepthFirst.getValue()),() -> "Failed with input: " + r.toString());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void broadClosure(IValueFactory vf, IBool forceDepthFirst) {
        IValue prev = vf.integer(0);
        var rBuilder = vf.setWriter();
        for (int i=1; i < 100; i++) {
            IValue next = vf.integer(i);
            if (i % 5 != 0) {
                rBuilder.appendTuple(prev, next);
            }
            prev = next;
        }
        var r = rBuilder.done().asRelation();

        assertEquals(slowClosure(vf, r),r.closure(forceDepthFirst.getValue()),() -> "Failed with input: " + r.toString());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void severalDeep(IValueFactory vf, IBool forceDepthFirst) {
        IValue prev = vf.integer(0);
        var rBuilder = vf.setWriter();
        for (int i=1; i < 100; i++) {
            IValue next = vf.integer(i);
            if (i % 50 != 0) {
                rBuilder.appendTuple(prev, next);
            }
            prev = next;
        }
        // now let's add some side paths
        prev = vf.integer(10);
        for (int i=11; i < 100; i+=2) {
            IValue next = vf.integer(i);
            if (i % 50 != 0) {
                rBuilder.appendTuple(prev, next);
            }
            prev = next;
        }
        var r = rBuilder.done().asRelation();

        assertEquals(slowClosure(vf, r),r.closure(forceDepthFirst.getValue()),() -> "Failed with input: " + r.toString());
    }

    private ISet slowClosure(IValueFactory vf, IRelation<ISet> r) {
        var prev = vf.set();
        ISet result = r.asContainer();
        while (!prev.equals(result)) {
            prev = result;
            result = result.union( r.compose(result.asRelation()));
        }
        return result;
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void carrierForTriples(@ExpectedType("rel[int,int,int]") ISet src) {
        ISet carrier = src.asRelation().carrier();
        
        for (int i = 0; i < 3; i++) {
            ISet column = src.asRelation().project(i);
            assertTrue(column.isSubsetOf(carrier));
        }
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void equalsIsKeyLocs(IValueFactory vf, @ExpectedType("rel[loc,loc]") ISet m, ISourceLocation key1, ISourceLocation key2) {
        IValue value = key1;
        ITuple key1Tuple = vf.tuple(key1, value);
        ISet m2 = m.insert(key1Tuple);
        
        if (key1.equals(key2)) {
            assertTrue(m2.asRelation().index(key2).contains(value));
        } else {
            ITuple key2Tuple = vf.tuple(key2, value);
            
            if (!m.asRelation().domain().contains(key2)) {
                assertEquals(m2.size() + 1, m2.insert(key2Tuple).size());
            }
        }
    }
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void equalsIsKeyLocFailure(IValueFactory vf, @GivenValue("{<|CwRj:///28030|,|KYs:///xr197/75|>,<|R:///|,|IkdM:///35/IR/72852/SYGC/C17|>,<|IhL:///dim/74/8b/65/xQgoC/0Rm#1|,|Q://45511U|>,<|Joq:///df/hh|,|dr:///u97|>}") ISet m,
                                                        @GivenValue("|oWxyV:///29/956|") ISourceLocation key1,
                                                        @GivenValue("|R:///|") ISourceLocation key2) {
        equalsIsKeyLocs(vf, m, key1, key2);                                      
    }
}
