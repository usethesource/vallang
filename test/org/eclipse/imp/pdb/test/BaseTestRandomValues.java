/*******************************************************************************
* Copyright (c) 2011 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Anya Helene Bagge (a.h.s.bagge@cwi.nl) - initial API and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.io.IValueBinaryReader;
import org.eclipse.imp.pdb.facts.io.IValueBinaryWriter;
import org.eclipse.imp.pdb.facts.io.IValueTextReader;
import org.eclipse.imp.pdb.facts.io.IValueTextWriter;
import org.eclipse.imp.pdb.facts.io.BinaryValueReader;
import org.eclipse.imp.pdb.facts.io.BinaryValueWriter;
import org.eclipse.imp.pdb.facts.io.StandardTextReader;
import org.eclipse.imp.pdb.facts.io.StandardTextWriter;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.test.random.*;


/**
 * Implements random testing of algebraic properties of the PDB values numeric
 * types (aka axiom-based testing or property-based testing).
 * 
 * Data is generated using the random data genererator in the random subpackage.
 * @author anya
 *
 */
abstract public class BaseTestRandomValues extends TestCase {
	protected IValueFactory vf;
	protected TypeFactory tf = TypeFactory.getInstance();
	protected IInteger INT_ONE;
	protected IInteger INT_ZERO;
	protected IRational RAT_ONE;
	protected IRational RAT_ZERO;
	protected IReal REAL_ONE;
	protected IReal REAL_ZERO;
	protected IReal MAX_ERROR_RATIO;
	protected IReal EPSILON;
	protected final int PRECISION = 100;
	// number of iterations per axiom
	protected int N = 1000;
	// TODO add more test cases
	protected List<IInteger> intTestSet;
	protected List<IRational> ratTestSet;
	protected List<IReal> realTestSet;
	private DataGenerator generator;
	protected static final boolean noisy = true;
	protected void setUp(IValueFactory factory) throws Exception {
		super.setUp();
		vf = factory;
		INT_ONE = vf.integer(1);
		INT_ZERO = vf.integer(0);
		RAT_ONE = vf.rational(1,1);
		RAT_ZERO = vf.rational(0,1);
		REAL_ONE = vf.real(1.0);
		REAL_ZERO = vf.real(0.0);
		// For approximate equality of reals  a ~= b:
		//   this is the max allowable ratio of (a-b) to max(a,b)
		MAX_ERROR_RATIO = vf.real(1e-70);
		//   this is the max allowed difference between a and b
		EPSILON = vf.real(1e-70);
		intTestSet = Arrays.asList(vf.integer(0), vf.integer(1), vf.integer(-1),
				vf.integer(2), vf.integer(-2), vf.integer(Long.MAX_VALUE),
				vf.integer(Long.MIN_VALUE), 
				vf.integer(Long.MAX_VALUE).multiply(vf.integer(Long.MAX_VALUE)),
				vf.integer(Long.MIN_VALUE).multiply(vf.integer(Long.MAX_VALUE)));
		ratTestSet = Arrays.asList(vf.rational(0,1), vf.rational(1,1), vf.rational(-1,1),
				vf.rational(1,2), vf.rational(2,1), vf.rational(Long.MAX_VALUE,Long.MIN_VALUE));
		realTestSet = new ArrayList<IReal>();
		for(IInteger i : intTestSet) {
			realTestSet.add(i.toReal());
		}
		for(IRational r : ratTestSet) {
			realTestSet.add(r.toReal());
		}
		realTestSet.addAll(Arrays.asList(vf.real(Double.MAX_VALUE), vf.real(Double.MIN_VALUE)));
		generator = new DataGenerator();
		generator.addGenerator(IInteger.class, intTestSet, new RandomIntegerGenerator(vf));
		generator.addGenerator(IRational.class, ratTestSet, new RandomRationalGenerator(vf));
		generator.addGenerator(IReal.class, realTestSet, new RandomRealGenerator(vf));
	}


	protected void assertEqual(IValue l, IValue r) {
		assertTrue("Expected " + l + " got " + r, l.isEqual(r));
	}

	/**
	 * Test that the difference between two reals is insignificant.
	 */
	protected void assertApprox(IReal l, IReal r) {
		assertTrue("Expected ~" + l + " got " + r + " (diff magnitude " + ((IReal)l.subtract(r).abs()).scale() + ")", approxEqual(l, r));
	}

	/**
	 * @return true if the two arguments are approximately equal
	 */
	protected boolean approxEqual(IReal l, IReal r) {
		if(l.equals(r))
			return true;  // really equal
		IReal max = (IReal) l.abs();
		if(((IReal)r.abs()).greater(max).getValue())
			max = (IReal) r.abs();
		
		IReal diff = (IReal) l.subtract(r).abs();
		if(diff.less(EPSILON).getValue())
			return true; // absolute difference is very small
		
		IReal relativeDiff = diff.divide(max, PRECISION);

		if(!relativeDiff.less(MAX_ERROR_RATIO).getValue())
			System.out.println("");

		// otherwise test relative difference
		return relativeDiff.less(MAX_ERROR_RATIO).getValue();
	}

	protected void assertEqual(Type l, Type r) {
		assertTrue("Expected " + l + " got " + r, l.equivalent(r));
	}

	public void testIO() throws IOException {
		if(noisy)
			System.out.println("Test I/O: " + "(" + getClass().getPackage().getName() + ")");

		ioHelperBin("PBF", new BinaryValueReader(), new BinaryValueWriter());
		ioHelperText("Text", new StandardTextReader(), new StandardTextWriter());
	}

	private void ioHelperText(String io, IValueTextReader reader, IValueTextWriter writer) throws IOException {
	  ioHelperText2(io + " Integers", reader, writer, new DataGenerator(generator, INumber.class, intTestSet, new RandomIntegerGenerator(vf)));
	  ioHelperText2(io + " Rationals", reader, writer, new DataGenerator(generator, INumber.class, ratTestSet, new RandomRationalGenerator(vf)));
	  ioHelperText2(io + " Reals", reader, writer, new DataGenerator(generator, INumber.class, realTestSet, new RandomRealGenerator(vf)));
	}
	
	private void ioHelperBin(String io, IValueBinaryReader reader, IValueBinaryWriter writer) throws IOException {
	  ioHelperBin2(io + " Integers", reader, writer, new DataGenerator(generator, INumber.class, intTestSet, new RandomIntegerGenerator(vf)));
	  ioHelperBin2(io + " Rationals", reader, writer, new DataGenerator(generator, INumber.class, ratTestSet, new RandomRationalGenerator(vf)));
	  ioHelperBin2(io + " Reals", reader, writer, new DataGenerator(generator, INumber.class, realTestSet, new RandomRealGenerator(vf)));
	}
	
	
	private void ioHelperText2(String typeName, IValueTextReader reader, IValueTextWriter writer, DataGenerator g) throws IOException {
		if(noisy)
			System.out.printf("  %-16s ", typeName + ":");
		int count = 0;
		for(INumber n : g.generate(INumber.class, N*10)) {
			ioHelperText3(reader, writer, n);
			count++;
		}
		if(noisy)
			System.out.println("" + count + " values");
	}
	
	private void ioHelperBin2(String typeName, IValueBinaryReader reader, IValueBinaryWriter writer, DataGenerator g) throws IOException {
		if(noisy)
			System.out.printf("  %-16s ", typeName + ":");
		int count = 0;
		for(INumber n : g.generate(INumber.class, N*10)) {
			ioHelperBin3(reader, writer, n);
			count++;
		}
		if(noisy)
			System.out.println("" + count + " values");
	}


	private void ioHelperText3(IValueTextReader reader, IValueTextWriter writer, INumber n)
			throws IOException, AssertionFailedError {
		StringWriter output = new StringWriter();
		writer.write(n, output);
		output.close();
		StringReader input = new StringReader(output.toString());
		IValue v = reader.read(vf, input);
		assertEqual(n, v);
	}
	
	private void ioHelperBin3(IValueBinaryReader reader, IValueBinaryWriter writer, INumber n)
			throws IOException, AssertionFailedError {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		writer.write(n, output);
		output.close();
		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		IValue v = reader.read(vf, input);
		assertEqual(n, v);
	}
	/**
	 * Run all axioms in the current class, i.e. all *public* methos
	 * with names starting with "axiom".
	 * 
	 * Constructs argument lists using the data generator, and calls
	 * the axioms using reflection.
	 * 
	 * @throws Throwable
	 */
	public void testAxioms() throws Throwable {
		Method[] methods = getClass().getMethods();
		long millis = System.currentTimeMillis();
		for(Method m : methods) {
			if(m.getName().startsWith("axiom")) {
				Class<?>[] params = m.getParameterTypes();
				// if at least one argument is an INumber, we want to
				// test the axiom for all numeric types
				if(hasINumber(params)) {
					if(noisy)
						System.out.print(m.getName() + "\n  Integers:  ");
					callAxiom(m, params, new Object[params.length], 0,
							new DataGenerator(generator, INumber.class, intTestSet, new RandomIntegerGenerator(vf)));
					if(noisy)
						System.out.print(" " + count + " calls\n" + m.getName() + "\n  Rationals: ");
					callAxiom(m, params, new Object[params.length], 0,
							new DataGenerator(generator, INumber.class, ratTestSet, new RandomRationalGenerator(vf)));
					if(noisy)
						System.out.print(" " + count + " calls\n" + m.getName() + "\n  Reals:     ");
					callAxiom(m, params, new Object[params.length], 0,
							new DataGenerator(generator, INumber.class, realTestSet, new RandomRealGenerator(vf)));
				}
				else {
					if(noisy) System.out.print(m.getName() + "\n          :  ");
					callAxiom(m, params, new Object[params.length], 0, generator);
				}
				if(noisy)
					System.out.println(" " + count + " calls");
			}
		}
		System.out.println("Axiom tests done in " + (System.currentTimeMillis()-millis) + " ms "
				+ "(" + getClass().getPackage().getName() + ")");
	}

	private boolean hasINumber(Class<?>[] params) {
		for(Class<?> p : params) {
			if(p.isAssignableFrom(INumber.class)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Keeps track of the number of times an axiom has been called.
	 */
	private int count = 0;
	
	/**
	 * @param m The axiom method
	 * @param params The list of parameter types
	 * @param args The argument list we've built so far
	 * @param k The number of arguments we've added so far
	 * @param g The data generator
	 * @throws Throwable if anything went wrong
	 */
	private <T> void callAxiom(Method m, Class<?>[] params, Object[] args, int k, DataGenerator g) 
			throws Throwable {
		if(k == 0)
			count = 0;
		if(params.length == k) { // we have a complete argument list
			try {
				m.invoke(this, args);
			}
			catch(InvocationTargetException e) {
				if(e.getCause() != null)
					throw e.getCause();
				else
					throw e;
			}
			count ++;
			if(noisy)
				if(count % 1000 == 0) System.out.print(".");
			if(noisy)
				if(count % 80000 == 0) System.out.print("\n             ");
		}
		else {
			// try with all possible values from the data generator for
			// this argument
			for(Object t : g.generate(params[k], numberOfValuesFor(params.length))) {
				args[k] = t;
				callAxiom(m, params, args, k+1, g);
			}
		}
	}
	
	/**
	 * Restrict the number of random values for long argument lists,
	 * or we'll end up with billions of calls.
	 * 
	 * @return the number of random values we should generate for an argument
	 * list of the given length.
	 */
	private int numberOfValuesFor(int length) {
		if(length >= 3)
			return N/70;
		else if(length == 2)
			return N/10;
		else
			return N*10;
	}


	/**
	 *  Closure: These operations should yield a result of the same type.
	 */
	public void axiomClosure(INumber a, INumber b) {
		if(a.signum() == 0 && b.signum() == 0)
			a.signum();
		if(a.getType().equivalent(b.getType())) {
			assertEqual(a.getType(), a.add(b).getType());
			assertEqual(a.getType(), a.multiply(b).getType());
			assertEqual(a.getType(), a.subtract(b).getType());
			assertEqual(a.getType(), a.abs().getType());
			assertEqual(a.getType(), a.negate().getType());			
		}
	}

	/**
	 * Associativity: addition and multiplication
	 * 
	 *  (Possibly not strictly true for reals.)
	 */
	public void axiomAssociativity(INumber a, INumber b, INumber c) {
		assertEqual(a.add(b.add(c)), a.add(b).add(c));
		assertEqual(a.multiply(b.multiply(c)), a.multiply(b).multiply(c));
	}


	/**
	 * Commutativity: addition and multiplication
	 */
	public void axiomCommutativity(INumber a, INumber b) {
		assertEqual(a.add(b), b.add(a));
		assertEqual(a.multiply(b), b.multiply(a));
	}


	/**
	 * 0 or 1 are identities for all the binary ops
	 */
	public void axiomIdentity(INumber a) {
		assertEqual(a, a.add(INT_ZERO));
		assertEqual(a, a.multiply(INT_ONE));
		assertEqual(a, a.subtract(INT_ZERO));
		if(a instanceof IInteger)
			assertEqual(a, ((IInteger)a).divide(INT_ONE));
		if(a instanceof IRational)
			assertEqual(a, ((IRational)a).divide(RAT_ONE));
		if(a instanceof IReal)
			assertEqual(a, ((IReal)a).divide(REAL_ONE, ((IReal)a).precision()));
	}

	/**
	 * Subtraction is inverse of addition.
	 * Division is inverse of non-integer multiplication.
	 */
	public void axiomInverse(INumber a) {
		if(a instanceof IInteger) {
			IInteger i = (IInteger)a;
			assertEqual(INT_ZERO, i.add(i.negate()));
			assertEqual(INT_ZERO, i.subtract(i));
			if(i.signum() != 0) {
				assertEqual(INT_ONE, i.divide(i));
			}
		}
		if(a instanceof IRational) {
			IRational r = (IRational)a;
			assertEqual(RAT_ZERO, r.add(r.negate()));
			assertEqual(RAT_ZERO, r.subtract(r));
			if(r.signum() != 0) {
				assertEqual(RAT_ONE, r.divide(r));
				assertEqual(RAT_ONE, r.multiply(RAT_ONE.divide(r)));
			}
		}
		if(a instanceof IReal) {
			IReal r = (IReal)a;
			// this should hold:
			assertEqual(REAL_ZERO, r.add(r.negate()));
			// this one only approximately
			try {
				assertApprox(REAL_ONE, r.divide(r, 80));
				assertApprox(REAL_ONE, r.multiply(REAL_ONE.divide(r, 80)));
			}
			catch(ArithmeticException e) {
				// ignore division by zero
			}
		}
	}


	/**
	 * Multiplication distributes over addition.
	 * 
	 * (Possibly not strictly true for reals.) 
	 */
	public void axiomDistributivity(INumber a, INumber b, INumber c) {
		assertEqual(a.multiply(b.add(c)),
				a.multiply(b).add(a.multiply(c)));
	}

	/**
	 * Relationship between compare() and the comparison functions,
	 * and between the various comparisons.
	 */
	public void axiomCompare(INumber a, INumber b) {
		int cmp = a.compare(b);
		assertEquals(cmp, -b.compare(a));
		assertEquals(cmp < 0, a.less(b).getValue());
		assertEquals(cmp > 0, a.greater(b).getValue());
		assertEquals(cmp == 0, a.isEqual(b));
		assertEquals(cmp <= 0, a.less(b).getValue() || a.isEqual(b));
		assertEquals(cmp >= 0, a.greater(b).getValue() || a.isEqual(b));

		assertEquals(a.less(b), b.greater(a));
		assertEquals(a.greaterEqual(b), b.lessEqual(a));
		assertEquals(a.lessEqual(b).getValue(), a.less(b).getValue() || a.isEqual(b));
		assertEquals(a.greaterEqual(b).getValue(), a.greater(b).getValue() || a.isEqual(b));

		assertEquals(a.isEqual(b), b.isEqual(a));
		assertTrue(a.isEqual(a));
		if(a.isEqual(b)) {
			assertEquals("" + a + ".hashCode() != " + b + ".hashCode()",
					a.hashCode(), b.hashCode());
			if(!(a instanceof IReal || b instanceof IReal))
				assertEquals("" + a + ".toString() != " + b + ".toString()",
						a.toString(), b.toString());
		}
		INumber c = b.abs();
		// add/subtract a non-negative number gives a greater/smaller or equal result
		assertTrue("" + a + " + " + c + " >= " + a, a.add(c).greaterEqual(a).getValue());
		assertTrue("" + a + " + -" + c + " >= " + a, a.add(c.negate()).lessEqual(a).getValue());
	}
	
	/**
	 *	This may not be strictly true for reals.
	 */
	public void axiomTransitivity(INumber a, INumber b, INumber c) {
		if(a.isEqual(b) && b.isEqual(c))
			assertTrue("" + a + " == " + b + " == " + c, a.isEqual(c));
		if(a.lessEqual(b).getValue() && b.lessEqual(c).getValue())
			assertTrue("" + a + " <= " + b + " <= " + c,
					a.lessEqual(c).getValue());
	}

	/**
	 *	Check that behavour of add/subtract/multiply/divide of integers is
	 * 	approximately the same as for reals
	 **/
	public void axiomRationalBehavior(IRational a, IRational b) {
		assertEqual(a, a.add(b).subtract(b));
		assertEqual(a, a.subtract(b).add(b));
		if(b.signum() != 0) {
			assertEqual(a, a.divide(b).multiply(b));
			assertEqual(a, a.multiply(b).divide(b));
		}
		assertEqual(a, a.negate().negate());
		assertEqual(a, a.abs().multiply(vf.integer(a.signum())));
		assertEqual(a, a.numerator().toRational().divide(a.denominator().toRational()));
		
		assertApprox(a.toReal().add(b.toReal()), a.add(b).toReal());
		assertApprox(a.toReal().subtract(b.toReal()), a.subtract(b).toReal());
		assertApprox(a.toReal().multiply(b.toReal()), a.multiply(b).toReal());
		try {
			assertApprox(a.toReal().divide(b.toReal(), PRECISION), a.divide(b).toReal());
		}
		catch(ArithmeticException e) {
		}
	}

	/**
	 * Check various behaviour + 
	 *	Check that behavour of add/subtract/multiply of rationals is
	 * the same as that for reals and rationals.
	 **/
	public void axiomIntegerBehavior(IInteger a, IInteger b) {
		assertEqual(a, a.add(b).subtract(b));
		assertEqual(a, a.subtract(b).add(b));
		if(b.signum() != 0) {
			assertEqual(a, a.divide(b).multiply(b).add(a.remainder(b)));
			assertEqual(a, a.multiply(b).divide(b));
		}
		assertEqual(a, a.negate().negate());
		assertEqual(a, a.abs().multiply(vf.integer(a.signum())));
		if(b.signum() != 0)
			assertTrue(a.mod(b.abs()).less(b.abs()).getValue());
		
		// check vs. real
		assertEqual(a.toReal().add(b.toReal()).toInteger(), a.add(b));
		assertEqual(a.toRational().add(b.toRational()).toInteger(), a.add(b));
		assertEqual(a.toReal().subtract(b.toReal()).toInteger(), a.subtract(b));
		assertEqual(a.toRational().subtract(b.toRational()).toInteger(), a.subtract(b));
		assertEqual(a.toReal().multiply(b.toReal()).toInteger(), a.multiply(b));
		assertEqual(a.toRational().multiply(b.toRational()).toInteger(), a.multiply(b));
	}

	public void axiomRealBehavior(IReal a, IReal b) {
		assertEqual(a, a.add(b).subtract(b));
		assertEqual(a, a.subtract(b).add(b));
		try {
			assertApprox(a, a.divide(b, PRECISION).multiply(b));
			assertApprox(a, a.multiply(b).divide(b, PRECISION));
		}
		catch(ArithmeticException e) {
		}
		assertEqual(a, a.negate().negate());
	}
}
