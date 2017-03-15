/** 
 * Copyright (c) 2016, Davy Landman, Centrum Wiskunde & Informatica (CWI) 
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package io.usethesource.vallang.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.usethesource.vallang.IReal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.Setup;
import io.usethesource.vallang.io.binary.util.StacklessStructuredVisitor;
import io.usethesource.vallang.io.binary.util.StructuredIValueVisitor;
import io.usethesource.vallang.io.reference.ReferenceStructuredIValueVisitor;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeStore;
import io.usethesource.vallang.util.RandomValues;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class StacklessStructuredVisitorTest {

  @Parameterized.Parameters
  public static Iterable<? extends Object> data() {
    return Setup.valueFactories();
  }

    private final IValueFactory vf;

    public StacklessStructuredVisitorTest(IValueFactory vf) {
        this.vf = vf;
    }

    @Test
    public void singleString() {
        testVisitStructure(vf.string("a"));
    }

    @Test
    public void nodeWithAnnotations() {
        Map<String, IValue> kws = new HashMap<>();
        kws.put("arg1", vf.integer(2));
        kws.put("arg2", vf.integer(3));
        testVisitStructure(vf.node("basicNode", new IValue[] {vf.integer(1), vf.string("a") }, kws));
    }


    @Test
    public void listWithTwoElements() {
        testVisitStructure(vf.list(vf.string("a"), vf.list()));
    }
    
    @Test
    public void correctOrderSmallValues() {
        for (IValue v: RandomValues.getTestValues(vf)) {
            testVisitStructure(v);
        }
    }

    @Test
    public void correctOrderSmallValuesSkipping() {
        for (IValue v: RandomValues.getTestValues(vf)) {
            testVisitStructureSkipped(v);
        }
    }
    
    @Test
    public void randomValuesCorrect() {
        TypeStore ts = new TypeStore();
        Type tp = RandomValues.addNameType(ts);
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            testVisitStructure(RandomValues.generate(tp, ts, vf, r, 10));
        }
    }
    @Test
    public void randomValuesCorrectSkipping() {
        TypeStore ts = new TypeStore();
        Type tp = RandomValues.addNameType(ts);
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            testVisitStructureSkipped(RandomValues.generate(tp, ts, vf, r, 10));
        }
    }

	private static class CollectAll implements StructuredIValueVisitor<RuntimeException> {
	    
	    public List<Object> result = new ArrayList<>();

	    @Override
	    public void enterNamedValues(String[] names, int numberOfNestedValues) {
	        result.add(Arrays.toString(names));
	        result.add(numberOfNestedValues);
	    }
	    
	    @Override
	    public void leaveNamedValue() {
            result.add("leave");
	    }
	    
        @Override
        public boolean enterConstructor(IConstructor cons, int children) throws RuntimeException {
            result.add(cons);
            result.add(children);
            return true;
        }

        @Override
        public void enterConstructorKeywordParameters() throws RuntimeException {
            result.add("kw");
        }

        @Override
        public void enterConstructorAnnotations() throws RuntimeException {
            result.add("an");
        }

        @Override
        public void leaveConstructor(IValue cons) throws RuntimeException {
            result.add("leave");
        }

        @Override
        public boolean enterNode(INode cons, int children) throws RuntimeException {
            result.add(cons);
            result.add(children);
            return true;
        }

        @Override
        public void enterNodeKeywordParameters() throws RuntimeException {
            result.add("kw");
        }

        @Override
        public void enterNodeAnnotations() throws RuntimeException {
            result.add("an");
        }

        @Override
        public void leaveNode(IValue node) throws RuntimeException {
            result.add("leave");
        }

        @Override
        public boolean enterList(IList lst, int children) throws RuntimeException {
            result.add(lst);
            result.add(children);
            return true;
        }

        @Override
        public void leaveList(IValue list) throws RuntimeException {
            result.add("leave");
        }

        @Override
        public boolean enterSet(ISet set, int elements) throws RuntimeException {
            result.add(set);
            result.add(elements);
            return true;
        }

        @Override
        public void leaveSet(IValue set) throws RuntimeException {
            result.add("leave");
        }

        @Override
        public boolean enterMap(IMap map, int elements) throws RuntimeException {
            result.add(map);
            result.add(elements);
            return true;
        }


        @Override
        public void leaveMap(IValue map) throws RuntimeException {
            result.add("leave");
        }

        @Override
        public boolean enterTuple(ITuple tuple, int arity) throws RuntimeException {
            result.add(tuple);
            result.add(arity);
            return true;
        }

        @Override
        public void leaveTuple(IValue tuple) throws RuntimeException {
            result.add("leave");
        }

        @Override
        public void visitString(IString o) throws RuntimeException {
            result.add(o);
        }

        @Override
        public void visitInteger(IInteger o) throws RuntimeException {
            result.add(o);
        }

        @Override
        public void visitReal(IReal o) throws RuntimeException {
            result.add(o);
        }

        @Override
        public void visitRational(IRational o) throws RuntimeException {
            result.add(o);
        }

        @Override
        public void visitSourceLocation(ISourceLocation o) throws RuntimeException {
            result.add(o);
        }

        @Override
        public void visitBoolean(IBool boolValue) throws RuntimeException {
            result.add(boolValue);
        }

        @Override
        public void visitDateTime(IDateTime o) throws RuntimeException {
            result.add(o);
        }
	}
	
	private static class CollectAllSkipping extends CollectAll {
	    @Override
	    public boolean enterList(IList lst, int children) throws RuntimeException {
	        super.enterList(lst, children);
	        return false;
	    }
	}

    private void testVisitStructure(IValue val) {
        CollectAll expected = new CollectAll();
        ReferenceStructuredIValueVisitor.accept(val, expected);
        CollectAll got = new CollectAll();
        StacklessStructuredVisitor.accept(val, got);
        compareLists(expected.result, got.result);
    }

    private void testVisitStructureSkipped(IValue val) {
        CollectAll expected = new CollectAllSkipping();
        ReferenceStructuredIValueVisitor.accept(val, expected);
        CollectAll got = new CollectAllSkipping();
        StacklessStructuredVisitor.accept(val, got);
        compareLists(expected.result, got.result);
    }

    private void compareLists(List<Object> expected, List<Object> actual) {
        assertEquals("We should visit the same amount of elements", expected.size(), actual.size());
        for (int i=0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i));
        }
    }

}
