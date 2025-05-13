package io.usethesource.vallang.specification;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.INumber;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.type.Type;

public class INumberTests {
    protected static final int PRECISION = 100;

    IInteger INT_ONE(IValueFactory vf) {
        return vf.integer(1);
    }

    IInteger INT_ZERO(IValueFactory vf) {
        return vf.integer(0);
    }

    IRational RAT_ONE(IValueFactory vf) {
        return vf.rational(1, 1);
    }

    IRational RAT_ZERO(IValueFactory vf) {
        return vf.rational(0, 1);
    }

    IReal REAL_ONE(IValueFactory vf) {
        return vf.real(1.0);
    }

    IReal REAL_ZERO(IValueFactory vf) {
        return vf.real(0.0);
    }

    /** For approximate equality of reals a ~= b:
    * this is the max allowable ratio of (a-b) to max(a,b)
    */
    IReal MAX_ERROR_RATIO(IValueFactory vf) {
        return vf.real(1e-15);
    }

    double DOUBLE_MAX_ERROR_RATIO() {
        return 1e-10;
    }

    // this is the max allowed difference between a and b
    IReal EPSILON(IValueFactory vf) {
        return vf.real(1e-10);
    }

    double DOUBLE_EPSILON() {
        return 1e-10;
    }

    protected void assertEqual(IValue l, IValue r) {
        Assertions.assertTrue(l.equals(r), "Expected " + l + " got " + r);
    }

    protected void assertEqualNumber(INumber l, INumber r) {
        Assertions.assertTrue(l.equal(r).getValue(), "Expected " + l + " got " + r);
    }

    protected void assertEqual(String message, IValue l, IValue r) {
        Assertions.assertTrue(l.equals(r), message + ": Expected " + l + " got " + r);
    }

    protected void assertEqualNumber(String message, INumber l, INumber r) {
        Assertions.assertTrue(l.equal(r).getValue(), message + ": Expected " + l + " got " + r);
    }

    /**
    * Test that the difference between two reals is insignificant.
    */
    protected void assertApprox(IValueFactory vf, IReal l, IReal r) {
        Assertions.assertTrue(approxEqual(vf, l, r), "Expected ~" + l + " got " + r + " (diff magnitude "
            + ((IReal) l.subtract(r).abs()).scale() + ")");
    }

    protected void assertApprox(double l, double r) {
        Assertions.assertTrue(approxEqual(l, r), "Expected ~" + l + " got " + r);
    }

    protected void assertApprox(String message, IValueFactory vf, IReal l, IReal r) {
        Assertions.assertTrue(approxEqual(vf, l, r), message + ": Expected ~" + l + " got " + r + " (diff magnitude "
            + ((IReal) l.subtract(r).abs()).scale() + ")");
    }

    protected void assertApprox(String message, double l, double r) {
        Assertions.assertTrue(approxEqual(l, r), message + ": Expected ~" + l + " got " + r);
    }

    /**
    * @return true if the two arguments are approximately equal
    */
    protected boolean approxEqual(IValueFactory vf, IReal l, IReal r) {
        if (l.equals(r))
            return true; // really equal
        IReal max = (IReal) l.abs();
        if (((IReal) r.abs()).greater(max).getValue())
            max = (IReal) r.abs();

        IReal diff = (IReal) l.subtract(r).abs();
        if (diff.less(EPSILON(vf)).getValue())
            return true; // absolute difference is very small

        IReal relativeDiff = diff.divide(max, PRECISION);

        if (!relativeDiff.less(MAX_ERROR_RATIO(vf)).getValue())
            System.out.println("");

        // otherwise test relative difference
        return relativeDiff.less(MAX_ERROR_RATIO(vf)).getValue();
    }

    /**
    * @return true if the two arguments are approximately equal
    */
    protected boolean approxEqual(double l, double r) {
        if (l == r)
            return true; // really equal
        double max = Math.abs(l);
        if (Math.abs(r) > max)
            max = Math.abs(r);

        double diff = Math.abs(l - r);
        if (diff < DOUBLE_EPSILON())
            return true; // absolute difference is very small

        double relativeDiff = diff / max;

        // otherwise test relative difference
        return relativeDiff < DOUBLE_MAX_ERROR_RATIO();
    }

    protected void assertEqual(Type l, Type r) {
        Assertions.assertTrue(l.equivalent(r), "Expected " + l + " got " + r);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void divByZero(IValueFactory vf, INumber a) {
        Assertions.assertThrows(ArithmeticException.class, () -> a.divide(vf.integer(0), 10));
    }

    /**
    * Relationship between compare() and the comparison functions, and between the various
    * comparisons.
    */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testCompare(INumber a, INumber b) {
        int cmp = a.compare(b);
        assertEquals(cmp == 0, b.compare(a) == 0); // negating and comparing directly isn't safe
        assertEquals(cmp < 0, b.compare(a) > 0);
        assertEquals(cmp > 0, b.compare(a) < 0);
        assertEquals(cmp < 0, a.less(b).getValue());
        assertEquals(cmp > 0, a.greater(b).getValue());
        assertEquals(cmp == 0, a.equal(b).getValue());
        assertEquals(cmp <= 0, a.less(b).getValue() || a.equal(b).getValue());
        assertEquals(cmp >= 0, a.greater(b).getValue() || a.equal(b).getValue());

        assertEquals(a.less(b), b.greater(a));
        assertEquals(a.greaterEqual(b), b.lessEqual(a));
        assertEquals(a.lessEqual(b).getValue(), a.less(b).getValue() || a.equal(b).getValue());
        assertEquals(a.greaterEqual(b).getValue(), a.greater(b).getValue() || a.equal(b).getValue());

        assertEquals(a.less(b).getValue() || a.greater(b).getValue(), !a.equal(b).getValue());
        assertEquals(a.equal(b).getValue(), b.equal(a).getValue());
        assertTrue(a.equal(a).getValue());

        if (a.equals(b) && a.getType() == b.getType()) {
            assertEquals( a.hashCode(), b.hashCode(), "" + a + ".hashCode() != " + b + ".hashCode()");

            if (!(a instanceof IReal || b instanceof IReal) && a.getType().equivalent(b.getType())) {
                assertEquals(a.toString(), b.toString(), "" + a + ".toString() != " + b + ".toString()");
            }
        }

        if (a.getType().equivalent(b.getType())) {
            INumber c = b.abs();
            // add/subtract a non-negative number gives a greater/smaller or equal result
            Assertions.assertTrue(a.add(c).greaterEqual(a).getValue(), "" + a + " + " + c + " >= " + a);
            Assertions.assertTrue(a.add(c.negate()).lessEqual(a).getValue(), "" + a + " + -" + c + " >= " + a);
        }
    }

    /**
    * Closure: These operations should yield a result of the same type.
    */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testClosure(INumber a, INumber b) {
        if (a.signum() == 0 && b.signum() == 0)
            a.signum();
        if (a.getType().equivalent(b.getType())) {
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
    * (Possibly not strictly true for reals.)
    */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testAssociativity(INumber a, INumber b, INumber c) {
        if (!(a instanceof IReal || b instanceof IReal || c instanceof IReal)) {
            assertEqualNumber(a.add(b.add(c)), a.add(b).add(c));
            assertEqualNumber(a.multiply(b.multiply(c)), a.multiply(b).multiply(c));
        }
    }

    /**
    * Commutativity: addition and multiplication
    */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testCommutativity(INumber a, INumber b) {
        assertEqualNumber(a.toString() + " + " + b.toString(), a.add(b), b.add(a));
        assertEqualNumber(a.toString() + " * " + b.toString(), a.multiply(b), b.multiply(a));
    }

    /**
    * 0 or 1 are identities for all the binary ops
    */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIdentity(IValueFactory vf, INumber a) {
        assertEqualNumber(a, a.add(INT_ZERO(vf)));
        assertEqualNumber(a, a.multiply(INT_ONE(vf)));
        assertEqualNumber(a, a.subtract(INT_ZERO(vf)));
        if (a instanceof IInteger)
            assertEqualNumber(a, ((IInteger) a).divide(INT_ONE(vf)));
        if (a instanceof IRational)
            assertEqualNumber(a, ((IRational) a).divide(RAT_ONE(vf)));
        if (a instanceof IReal)
            assertEqualNumber(a, ((IReal) a).divide(REAL_ONE(vf), ((IReal) a).precision()));
    }

    /**
    * Subtraction is inverse of addition. Division is inverse of non-integer multiplication.
    */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testInverse(IValueFactory vf, INumber a) {
        if (a instanceof IInteger) {
            IInteger i = (IInteger) a;
            assertEqualNumber(INT_ZERO(vf), i.add(i.negate()));
            assertEqualNumber(INT_ZERO(vf), i.subtract(i));
            if (i.signum() != 0) {
                assertEqualNumber(INT_ONE(vf), i.divide(i));
            }
        }
        if (a instanceof IRational) {
            IRational r = (IRational) a;
            assertEqualNumber(RAT_ZERO(vf), r.add(r.negate()));
            assertEqualNumber(RAT_ZERO(vf), r.subtract(r));
            if (r.signum() != 0) {
                assertEqualNumber(RAT_ONE(vf), r.divide(r));
                assertEqualNumber(RAT_ONE(vf), r.multiply(RAT_ONE(vf).divide(r)));
            }
        }
        if (a instanceof IReal) {
            IReal r = (IReal) a;
            // this should hold:
            assertEqualNumber(REAL_ZERO(vf), r.add(r.negate()));
            // this one only approximately
            try {
                assertApprox(vf, REAL_ONE(vf), r.divide(r, 80));
                assertApprox(vf, REAL_ONE(vf), r.multiply(REAL_ONE(vf).divide(r, 80)));
            } catch (ArithmeticException e) {
                // ignore division by zero
            }
        }
    }

    /**
    * Multiplication distributes over addition.
    *
    * (Possibly not strictly true for reals.)
    */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testDistributivity(INumber a, INumber b, INumber c) {
        if (!(a instanceof IReal || b instanceof IReal || c instanceof IReal)) {
            assertEqualNumber(String.format("a=%s, b=%s, c=%s", a.toString(), b.toString(), c.toString()),
                a.multiply(b.add(c)), a.multiply(b).add(a.multiply(c)));
        } else {
            // assertApprox(String.format("a=%s, b=%s, c=%s", a.toString(), b.toString(), c.toString()),
            // a.multiply(b.add(c)).toReal(), a.multiply(b).add(a.multiply(c)).toReal());
        }
    }

    /**
    * This may not be strictly true for reals.
    */
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testTransitivity(INumber a, INumber b, INumber c) {
        if (a.equal(b).getValue() && b.equal(c).getValue())
            Assertions.assertTrue(a.equal(c).getValue(), "" + a + " == " + b + " == " + c);
        if (a.lessEqual(b).getValue() && b.lessEqual(c).getValue())
            Assertions.assertTrue(a.lessEqual(c).getValue(), "" + a + " <= " + b + " <= " + c);
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testNoEqualInt(IInteger i) {
        assertFalse(i.toReal(PRECISION).equals(i));
        assertTrue(i.toReal(PRECISION).equal(i).getValue());
        assertFalse(i.toRational().equals(i));
        assertTrue(i.toRational().equal(i).getValue());
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testNoEqualRat(IRational i) {
        assertFalse(i.toReal(PRECISION).equals(i));
        assertTrue(i.toReal(PRECISION).equal(i).getValue());
        assertFalse(i.toInteger().equals(i));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testNoEqualReal(IReal i) {
        assertFalse(i.toInteger().equals(i));
    }

    /**
    * Check that behavour of add/subtract/multiply/divide of integers is approximately the same as
    * for reals
    **/
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRationalBehavior(IValueFactory vf, IRational a, IRational b) {
        assertEqualNumber(a, a.add(b).subtract(b));
        assertEqualNumber(a, a.subtract(b).add(b));
        if (b.signum() != 0) {
            assertEqualNumber(a, a.divide(b).multiply(b));
            assertEqualNumber(a, a.multiply(b).divide(b));
        }
        assertEqualNumber(a, a.negate().negate());
        assertEqualNumber(a, a.abs().multiply(vf.integer(a.signum())));
        assertEqualNumber(a, a.numerator().toRational().divide(a.denominator().toRational()));

        assertApprox(a.doubleValue() + b.doubleValue(), a.add(b).doubleValue());
        assertApprox(a.doubleValue() - b.doubleValue(), a.subtract(b).doubleValue());
        assertApprox(a.doubleValue() * b.doubleValue(), a.multiply(b).doubleValue());
        try {
            assertApprox(a.doubleValue() / b.doubleValue(), a.divide(b).doubleValue());
        } catch (ArithmeticException e) {
        }
    }

    /**
    * Check various behaviour + Check that behavour of add/subtract/multiply of rationals is the same
    * as that for reals and rationals.
    **/
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testIntegerBehavior(IValueFactory vf, IInteger a, IInteger b) {
        assertEqualNumber(a, a.add(b).subtract(b));
        assertEqualNumber(a, a.subtract(b).add(b));
        if (b.signum() != 0) {
            assertEqualNumber(a, a.divide(b).multiply(b).add(a.remainder(b)));
            assertEqualNumber(a, a.multiply(b).divide(b));
        }
        assertEqualNumber(a, a.negate().negate());
        assertEqualNumber(a, a.abs().multiply(vf.integer(a.signum())));
        if (b.signum() != 0)
            assertTrue(a.mod(b.abs()).less(b.abs()).getValue());

        // check vs. rational
        assertEqualNumber(a.toRational().add(b.toRational()).toInteger(), a.add(b));
        assertEqualNumber(a.toRational().subtract(b.toRational()).toInteger(), a.subtract(b));
        assertEqualNumber(a.toRational().multiply(b.toRational()).toInteger(), a.multiply(b));
    }

    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void testRealBehavior(IValueFactory vf, IReal a, IReal b) {
        assertApprox(vf, a, a.add(b).subtract(b));
        assertApprox(vf, a, a.subtract(b).add(b));
        try {
            assertApprox(vf, a, a.divide(b, PRECISION).multiply(b));
            assertApprox(vf, a, a.multiply(b).divide(b, PRECISION));
        } catch (ArithmeticException e) {
        }
        assertEqualNumber(a, a.negate().negate());
    }
}
