package org.eclipse.imp.pdb.test;

import java.math.BigDecimal;

import junit.framework.TestCase;

import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.reference.ValueFactory;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.junit.Test;

public class BigDecimalOperations extends TestCase {

	private static TypeStore ts = new TypeStore();
	private static TypeFactory tf = TypeFactory.getInstance();
	private static IValueFactory vf = ValueFactory.getInstance();

	private static void assertClose(IReal param, IReal actual, double expected) {
		assertTrue("failed for "+param+" real:" + actual + "double: " + expected, Math.abs(actual.doubleValue() - expected) < 0.00001);
	}

	public void testSinExtensive() {
		IReal start = vf.real("-100");
		IReal stop = start.negate();
		IReal increments = vf.real("0.1");
		for (IReal param = start; !stop.less(param).getValue(); param = param.add(increments)) {
			assertClose(param, param.sin(vf.getPrecision()), Math.sin(param.doubleValue()));
		}
	}

	public void testCosExtensive() {
		IReal start = vf.real("-100");
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

}
