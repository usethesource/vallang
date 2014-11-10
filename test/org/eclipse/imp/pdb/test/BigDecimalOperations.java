package org.eclipse.imp.pdb.test;

import junit.framework.TestCase;

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.reference.ValueFactory;

public class BigDecimalOperations extends TestCase {

	private static IValueFactory vf = ValueFactory.getInstance();

	private static void assertClose(INumber param, IReal actual, double expected) {
		assertTrue("failed for "+param+" real:" + actual + "double: " + expected, Math.abs(actual.doubleValue() - expected) < 0.00001);
	}

	public void testSinExtensive() {
		IReal start = vf.real(-100);
		IReal stop = start.negate();
		IReal increments = vf.real("0.1");
		for (IReal param = start; !stop.less(param).getValue(); param = param.add(increments)) {
			assertClose(param, param.sin(vf.getPrecision()), Math.sin(param.doubleValue()));
		}
	}

	public void testCosExtensive() {
		IReal start = vf.real(-100);
		IReal stop = start.negate();
		IReal increments = vf.real("0.1");
		for (IReal param = start; !stop.less(param).getValue(); param = param.add(increments)) {
			assertClose(param, param.cos(vf.getPrecision()), Math.cos(param.doubleValue()));
		}
	}

	public void testTanExtensive() {
		IReal start = vf.pi(vf.getPrecision()).divide(vf.real(2.0), vf.getPrecision()).negate();
		IReal stop = start.negate();
		IReal increments = vf.real("0.01");
		
		// around pi/2 tan is undefined so we skip checking around that.
		start = start.add(increments);
		stop = stop.subtract(increments);
		for (IReal param = start; !stop.less(param).getValue() ; param = param.add(increments)) {
			assertClose(param, param.tan(vf.getPrecision()), Math.tan(param.doubleValue()));
		}
	}
	
	private static double log2(double x) {
		return Math.log(x)/Math.log(2);
	}
	
	public void testLog2Extensive() {
		IReal start = vf.real(0);
		IReal stop = vf.real(100);
		IReal increments = vf.real("0.1");
		start = start.add(increments);
		for (IReal param = start; !stop.less(param).getValue(); param = param.add(increments)) {
			assertClose(param, param.log(vf.integer(2), vf.getPrecision()), log2(param.doubleValue()));
		}
	}
	public void testLog10Extensive() {
		IReal start = vf.real(0);
		IReal stop = vf.real(100);
		IReal increments = vf.real("0.1");
		start = start.add(increments);
		for (IReal param = start; !stop.less(param).getValue(); param = param.add(increments)) {
			assertClose(param, param.log(vf.integer(10), vf.getPrecision()), Math.log10(param.doubleValue()));
		}
	}

	public void testLnExtensive() {
		IReal start = vf.real(0);
		IReal stop = vf.real(100);
		IReal increments = vf.real("0.1");
		start = start.add(increments);
		for (IReal param = start; !stop.less(param).getValue(); param = param.add(increments)) {
			assertClose(param, param.ln(vf.getPrecision()), Math.log(param.doubleValue()));
		}
	}	

	public void testPowAllNumbers() {
		IReal start = vf.real(-10);
		IReal stop = start.negate();
		IReal increments = vf.real("0.1");
		IReal x = vf.pi(10);

		for (IReal param = start; !stop.less(param).getValue(); param = param.add(increments)) {
			assertClose(param, x.pow(param, vf.getPrecision()), Math.pow(x.doubleValue(), param.doubleValue()));
		}
	}
	
	public void testPowNaturalNumbers() {
		IInteger start = vf.integer(-10);
		IInteger stop = start.negate();
		IInteger increments = vf.integer(1);
		IReal x = vf.pi(10);

		for (IInteger param = start; !stop.less(param).getValue(); param = param.add(increments)) {
			assertClose(param, x.pow(param), Math.pow(x.doubleValue(), param.doubleValue()));
		}
	}
	
	

}
