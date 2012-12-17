/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.test;

import java.util.Iterator;

import junit.framework.TestCase;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListRelation;
import org.eclipse.imp.pdb.facts.IListRelationWriter;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public abstract class BaseTestListRelation extends TestCase {
    private IValueFactory vf;
	private TypeFactory tf;
	private IValue[] integers;
	private ITuple[] integerTuples;
	private IList listOfIntegers;
	private IListRelation integerListRelation;
	private IValue[] doubles;
	private IList listOfDoubles;
	private IListRelation doubleListRelation;
	private ITuple[] doubleTuples;
    
	protected void setUp(IValueFactory factory) throws Exception {
		super.setUp();
		vf = factory;
		tf = TypeFactory.getInstance();
		
		integers = new IValue[5];
		IListWriter lw = vf.listWriter(tf.integerType());
		
		for (int i = 0; i < integers.length; i++) {
			IValue iv = vf.integer(i);
			integers[i] = iv;
			lw.insert(iv);
		}
		listOfIntegers = lw.done();
		
		doubles = new IValue[10];
		IListWriter lw2 = vf.listWriter(tf.realType());
		
		for (int i = 0; i < doubles.length; i++) {
			IValue iv = vf.real(i);
			doubles[i] = iv;
			lw2.insert(iv);
		}
		listOfDoubles = lw2.done();
		IListRelationWriter rw = vf.listRelationWriter(tf.tupleType(tf.integerType(), tf.integerType()));
		integerTuples = new ITuple[integers.length * integers.length];
		
		for (int i = 0; i < integers.length; i++) {
			for (int j = 0; j < integers.length; j++) {
				ITuple t = vf.tuple(integers[i], integers[j]);
				integerTuples[i * integers.length + j] = t;
				rw.insert(t);
			}
		}
		integerListRelation = rw.done();
		
		IListRelationWriter rw2 = vf.listRelationWriter(tf.tupleType(tf.realType(), tf.realType()));
		doubleTuples = new ITuple[doubles.length * doubles.length];
		
		for (int i = 0; i < doubles.length; i++) {
			for (int j = 0; j < doubles.length; j++) {
				ITuple t = vf.tuple(doubles[i], doubles[j]);
				doubleTuples[i * doubles.length + j] = t;
				rw2.insert(t);
			}
		}
		doubleListRelation = rw2.done();
	}

	public void testIsEmpty() {
		if (integerListRelation.isEmpty()) {
			fail("integerRelation is not empty");
		}
		
		if (!vf.relation(tf.tupleType(tf.integerType())).isEmpty()) {
			fail("this relation should be empty");
		}
		
		IRelation emptyRel = vf.relation();
		if (!emptyRel.isEmpty()) {
			fail("empty relation is not empty?");
		}
		if (!emptyRel.getType().isRelationType()) {
			fail("empty relation should have relation type");
		}
		
		
	}

	public void testSize() {
		if (integerListRelation.length() != integerTuples.length) {
			fail("relation size is not correct");
		}
	}

	public void testArity() {
		if (integerListRelation.arity() != 2) {
			fail("arity should be 2");
		}
	}

	public void testProductIRelation() {
		IListRelation prod = integerListRelation.product(integerListRelation);
		
		if (prod.arity() != 2 ) {
			fail("arity of product should be 2");
		}
		
		if (prod.length() != integerListRelation.length() * integerListRelation.length()) {
			fail("size of product should be square of size of integerRelation");
		}
	}

	public void testProductISet() {
		IListRelation prod = integerListRelation.product(listOfIntegers);
		
		if (prod.arity() != 2) {
			fail("arity of product should be 2");
		}
		
		if (prod.length() != integerListRelation.length() * listOfIntegers.length()) {
			fail("size of product should be square of size of integerRelation");
		}
	}

	public void testClosure() {
		try {
			if (!integerListRelation.closure().isEqual(integerListRelation)) {
				fail("closure adds extra tuples?");
			}
		} catch (FactTypeUseException e) {
			fail("integerRelation is reflexive, so why an error?");
		}
		
		try {
			IRelation rel = vf.relation(tf.tupleType(tf.integerType(), tf.integerType()));
			rel.closure();
		}
		catch (FactTypeUseException e) {
			fail("reflexivity with subtyping is allowed");
		}
		
		try {
			IRelation rel = vf.relation(tf.tupleType(tf.integerType(), tf.realType()));
			rel.closure();
			fail("relation is not reflexive but no type error thrown");
		}
		catch (FactTypeUseException e) {
			// this should happen
		}
		
		
		
		try {
			ITuple t1 = vf.tuple(integers[0], integers[1]);
			ITuple t2 = vf.tuple(integers[1], integers[2]);
			ITuple t3 = vf.tuple(integers[2], integers[3]);
			ITuple t4 = vf.tuple(integers[0], integers[2]);
			ITuple t5 = vf.tuple(integers[1], integers[3]);
			ITuple t6 = vf.tuple(integers[0], integers[3]);
			
			IRelation test = vf.relation(t1, t2, t3);
			IRelation closed = test.closure();
			
			if (closed.arity() != test.arity()) {
				fail("closure should produce relations of same arity");
			}
			
			if (closed.size() != 6) {
				fail("closure contains too few elements");
			}
			
			if (!closed.intersect(test).isEqual(test)) {
				fail("closure should contain all original elements");
			}
			
			if (!closed.contains(t4) || !closed.contains(t5) || !closed.contains(t6)) {
				fail("closure does not contain required elements");
			}
		
		} catch (FactTypeUseException e) {
			fail("this should all be type correct");
		}
	}

	public void testCompose() {
		try {
			IListRelation comp = integerListRelation.compose(integerListRelation);
			
			if (comp.arity() != integerListRelation.arity() * 2 - 2) {
				fail("composition is a product with the last column of the first relation and the first column of the last relation removed");
			}
			
			if (comp.length() != integerListRelation.length()) {
				fail("numner of expected tuples is off");
			}
		} catch (FactTypeUseException e) {
			fail("the above should be type correct");
		}
		
		try {
			ITuple t1 = vf.tuple(integers[0], doubles[0]);
			ITuple t2 = vf.tuple(integers[1], doubles[1]);
			ITuple t3 = vf.tuple(integers[2], doubles[2]);
			IRelation rel1 = vf.relation(t1, t2, t3);

			ITuple t4 = vf.tuple(doubles[0], integers[0]);
			ITuple t5 = vf.tuple(doubles[1], integers[1]);
			ITuple t6 = vf.tuple(doubles[2], integers[2]);
			IRelation rel2 = vf.relation(t4, t5, t6);
			
			ITuple t7 = vf.tuple(integers[0], integers[0]);
			ITuple t8 = vf.tuple(integers[1], integers[1]);
			ITuple t9 = vf.tuple(integers[2], integers[2]);
			IRelation rel3 = vf.relation(t7, t8, t9);
			
			try {
			  vf.relation(vf.tuple(doubles[0],doubles[0])).compose(rel1);
			  fail("relations should not be composable");
			}
			catch (FactTypeUseException e) {
				// this should happen
			}
			
			IRelation comp = rel1.compose(rel2);
			
			if (!comp.isEqual(rel3)) {
				fail("composition does not produce expected result");
			}
		} catch (FactTypeUseException e) {
			fail("the above should be type correct");
		}
	}

	public void testContains() {
		try {
			for (ITuple t : integerTuples) {
				if (!integerListRelation.contains(t)) {
					fail("contains returns false instead of true");
				}
			}
		} catch (FactTypeUseException e) {
			fail("this should be type correct");
		}
	}

	public void testInsert() {
		try {
			IList rel = integerListRelation.insert(vf.tuple(vf.integer(0),vf.integer(0)));
			
			if (!rel.isEqual(integerListRelation)) {
				fail("insert into a relation of an existing tuple should not change the relation");
			}
			
			IListRelationWriter relw3 = vf.listRelationWriter(tf.tupleType(tf.integerType(), tf.integerType()));
			relw3.insertAll(integerListRelation);
			IListRelation rel3 = relw3.done();
			 
			final ITuple tuple = vf.tuple(vf.integer(100), vf.integer(100));
			IList rel4 = rel3.insert(tuple);
			
			if (rel4.length() != integerListRelation.length() + 1) {
				fail("insert failed");
			}
			
			if (!rel4.contains(tuple)) {
				fail("insert failed");
			}
			
		} catch (FactTypeUseException e) {
			fail("the above should be type correct");
		}
	}

	public void testIntersectIRelation() {
		IRelation empty1 = vf.relation(tf.tupleType(tf.integerType()));
		IRelation empty2 = vf.relation(tf.tupleType(tf.realType()));
		
		try {
			final IRelation intersection = empty1.intersect(empty2);
			if (!intersection.isEmpty()) {
				fail("empty intersection failed");
			}
			
			Type type = intersection.getType();
			if (!type.getFieldType(0).isNumberType()) {
				fail("intersection should produce lub types");
			}
		} catch (FactTypeUseException e) {
		    fail("intersecting types which have a lub should be possible");
		}
		
		try {
			if (!integerListRelation.intersect(doubleListRelation).isEmpty()) {
				fail("non-intersecting relations should produce empty intersections");
			}

			IRelation oneTwoThree = vf.relation(integerTuples[0],
					integerTuples[1], integerTuples[2]);
			IRelation threeFourFive = vf.relation(integerTuples[2],
					integerTuples[3], integerTuples[4]);
			IRelation result = vf.relation(integerTuples[2]);

			if (!oneTwoThree.intersect(threeFourFive).isEqual(result)) {
				fail("intersection failed");
			}
			if (!threeFourFive.intersect(oneTwoThree).isEqual(result)) {
				fail("intersection should be commutative");
			}
			
			if (!oneTwoThree.intersect(vf.relation(tf.tupleType(tf.integerType(),tf.integerType()))).isEmpty()) {
				fail("intersection with empty set should produce empty");
			}

		} catch (FactTypeUseException e) {
			fail("the above should all be type safe");
		} 
	}

	public void testIntersectISet() {
		IRelation empty1 = vf.relation(tf.tupleType(tf.integerType()));
		ISet empty2 = vf.set(tf.tupleType(tf.realType()));
		
		try {
			final IRelation intersection = empty1.intersect(empty2);
			if (!intersection.isEmpty()) {
				fail("empty intersection failed");
			}
			
			Type type = intersection.getType();
			if (!type.getFieldType(0).isNumberType()) {
				fail("intersection should produce lub types");
			}
		} catch (FactTypeUseException e) {
		    fail("intersecting types which have a lub should be possible");
		}
		
		try {
			if (!integerListRelation.intersect(doubleListRelation).isEmpty()) {
				fail("non-intersecting relations should produce empty intersections");
			}

			IRelation oneTwoThree = vf.relation(integerTuples[0],
					integerTuples[1], integerTuples[2]);
			ISet threeFourFive = vf.set(integerTuples[2],
					integerTuples[3], integerTuples[4]);
			IRelation result = vf.relation(integerTuples[2]);

			if (!oneTwoThree.intersect(threeFourFive).isEqual(result)) {
				fail("intersection failed");
			}
			if (!threeFourFive.intersect(oneTwoThree).isEqual(result)) {
				fail("intersection should be commutative");
			}
			
			if (!oneTwoThree.intersect(vf.relation(tf.tupleType(tf.integerType(),tf.integerType()))).isEmpty()) {
				fail("intersection with empty set should produce empty");
			}

		} catch (FactTypeUseException e) {
			fail("the above should all be type safe");
		} 
	}


	public void testSubtractIRelation() {
		IRelation empty1 = vf.relation(tf.tupleType(tf.integerType()));
		IRelation empty2 = vf.relation(tf.tupleType(tf.realType()));
		
		try {
			final IRelation diff = empty1.subtract(empty2);
			if (!diff.isEmpty()) {
				fail("empty diff failed");
			}
			
		} catch (FactTypeUseException e) {
		    fail("subtracting types which have a lub should be possible");
		}
		
		try {
			IRelation oneTwoThree = vf.relation(integerTuples[0],
					integerTuples[1], integerTuples[2]);
			IRelation threeFourFive = vf.relation(integerTuples[2],
					integerTuples[3], integerTuples[4]);
			IRelation result1 = vf.relation(integerTuples[0],integerTuples[1]);
			IRelation result2 = vf.relation(integerTuples[3],integerTuples[4]);

			if (!oneTwoThree.subtract(threeFourFive).isEqual(result1)) {
				fail("subtraction failed");
			}
			if (!threeFourFive.subtract(oneTwoThree).isEqual(result2)) {
				fail("subtraction failed");
			}
			
			IRelation empty3 = vf.relation(tf.tupleType(tf.integerType(),tf.integerType()));
			if (!empty3.subtract(threeFourFive).isEmpty()) {
				fail("subtracting from empty set should produce empty");
			}

		} catch (FactTypeUseException e) {
			fail("the above should all be type safe");
		} 
	}

	public void testSubtractISet() {
		IRelation empty1 = vf.relation(tf.tupleType(tf.integerType()));
		ISet empty2 = vf.set(tf.tupleType(tf.realType()));
		
		try {
			final IRelation diff = empty1.subtract(empty2);
			if (!diff.isEmpty()) {
				fail("empty diff failed");
			}
			
		} catch (FactTypeUseException e) {
		    fail("subtracting types which have a lub should be possible");
		}
		
		try {
			IRelation oneTwoThree = vf.relation(integerTuples[0],
					integerTuples[1], integerTuples[2]);
			ISet threeFourFive = vf.set(integerTuples[2],
					integerTuples[3], integerTuples[4]);
			IRelation result1 = vf.relation(integerTuples[0],integerTuples[1]);

			if (!oneTwoThree.subtract(threeFourFive).isEqual(result1)) {
				fail("subtraction failed");
			}
			
			IRelation empty3 = vf.relation(tf.tupleType(tf.integerType(),tf.integerType()));
			if (!empty3.subtract(threeFourFive).isEmpty()) {
				fail("subtracting from empty set should produce empty");
			}

		} catch (FactTypeUseException e) {
			fail("the above should all be type safe");
		}
	}

	public void testUnionIRelation() {
		IListRelation empty1 = vf.listRelation(tf.tupleType(tf.integerType()));
		IListRelation empty2 = vf.listRelation(tf.tupleType(tf.realType()));
		
		try {
			final IRelation union = empty1.union(empty2);
			if (!union.isEmpty()) {
				fail("empty union failed");
			}
			
			Type type = union.getType();
			if (!type.getFieldType(0).isNumberType()) {
				fail("union should produce lub types");
			}
		} catch (FactTypeUseException e) {
		    fail("union types which have a lub should be possible");
		}
		
		try {
			if (integerListRelation.union(doubleListRelation).length() != integerListRelation.length() + doubleListRelation.length())  {
				fail("non-intersecting non-intersectiopn relations should produce relation that is the sum of the sizes");
			}

			IRelation oneTwoThree = vf.relation(integerTuples[0],
					integerTuples[1], integerTuples[2]);
			IRelation threeFourFive = vf.relation(integerTuples[2],
					integerTuples[3], integerTuples[4]);
			IRelation result = vf.relation(integerTuples[0],
					integerTuples[1], integerTuples[2], integerTuples[3], integerTuples[4]);

			if (!oneTwoThree.union(threeFourFive).isEqual(result)) {
				fail("union failed");
			}
			if (!threeFourFive.union(oneTwoThree).isEqual(result)) {
				fail("union should be commutative");
			}
			
			if (!oneTwoThree.union(vf.relation(tf.tupleType(tf.integerType(),tf.integerType()))).isEqual(oneTwoThree)) {
				fail("union with empty set should produce same set");
			}

		} catch (FactTypeUseException e) {
			fail("the above should all be type safe");
		} 
	}

	public void testUnionISet() {
		IRelation empty1 = vf.relation(tf.tupleType(tf.integerType()));
		ISet empty2 = vf.set(tf.tupleType(tf.realType()));
		
		try {
			final IRelation union = empty1.union(empty2);
			if (!union.isEmpty()) {
				fail("empty union failed");
			}
			
			Type type = union.getType();
			if (!type.getFieldType(0).isNumberType()) {
				fail("union should produce lub types");
			}
		} catch (FactTypeUseException e) {
		    fail("union types which have a lub should be possible");
		}
		
		try {
			if (integerListRelation.union(doubleListRelation).length() != integerListRelation.length() + doubleListRelation.length())  {
				fail("non-intersecting non-intersectiopn relations should produce relation that is the sum of the sizes");
			}

			IRelation oneTwoThree = vf.relation(integerTuples[0],
					integerTuples[1], integerTuples[2]);
			ISet threeFourFive = vf.set(integerTuples[2],
					integerTuples[3], integerTuples[4]);
			IRelation result = vf.relation(integerTuples[0],
					integerTuples[1], integerTuples[2], integerTuples[3], integerTuples[4]);

			if (!oneTwoThree.union(threeFourFive).isEqual(result)) {
				fail("union failed");
			}
			if (!threeFourFive.union(oneTwoThree).isEqual(result)) {
				fail("union should be commutative");
			}
			
			if (!oneTwoThree.union(vf.set(tf.tupleType(tf.integerType(),tf.integerType()))).isEqual(oneTwoThree)) {
				fail("union with empty set should produce same set");
			}

		} catch (FactTypeUseException e) {
			fail("the above should all be type safe");
		} 
	}

	public void testIterator() {
		try {
			Iterator<IValue> it = integerListRelation.iterator();

			int i;
			for (i = 0; it.hasNext(); i++) {
				ITuple t = (ITuple) it.next();

				if (!integerListRelation.contains(t)) {
					fail("iterator produces strange elements?");
				}
			}
			
			if (i != integerListRelation.length()) {
				fail("iterator skipped elements");
			}
		} catch (FactTypeUseException e) {
			fail("the above should be type correct");
		}
	}

	public void testCarrier() {
		IList carrier = integerListRelation.carrier();
		
		if (!carrier.isEqual(listOfIntegers)) {
			fail("carrier should be equal to this set");
		}
	
		try {
			ITuple t1 = vf.tuple(integers[0], doubles[0]);
			ITuple t2 = vf.tuple(integers[1], doubles[1]);
			ITuple t3 = vf.tuple(integers[2], doubles[2]);
			IListRelation rel1 = vf.listRelation(t1, t2, t3);
			
			IList carrier1 = rel1.carrier();
			
			if (carrier1.getElementType() != tf.numberType()) {
				fail("expected number type on carrier");
			}
			
			if (carrier1.length() != 6) {
				fail("carrier does not contain all elements");
			}
			
			if (carrier1.intersect(listOfIntegers).length() != 3) {
				fail("integers should be in there still");
			}
			
			if (carrier1.intersect(listOfDoubles).length() != 3) {
				fail("doubles should be in there still");
			}
		} catch (FactTypeUseException e) {
			fail("the above should be type correct");
		}
		
	}
}
