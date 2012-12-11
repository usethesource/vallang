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

import junit.framework.TestCase;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeDeclarationException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;

public abstract class BaseTestAnnotations extends TestCase {
    private IValueFactory vf;
    private TypeFactory tf = TypeFactory.getInstance();
    private TypeStore ts = new TypeStore();
    private Type E;
    private Type N;
    
	protected void setUp(IValueFactory factory) throws Exception {
		super.setUp();
		vf = factory;
		E = tf.abstractDataType(ts, "E");
		N = tf.constructor(ts, E, "n", tf.integerType());
		ts.declareAnnotation(E, "x", tf.integerType());
	}
	
	public void testDeclarationOnNonAllowedType() {
		try {
			ts.declareAnnotation(tf.integerType(), "a", tf.integerType());
		}
		catch (FactTypeDeclarationException e) {
			// this should happen
		}
		try {
			ts.declareAnnotation(tf.realType(), "a", tf.integerType());
		}
		catch (FactTypeDeclarationException e) {
			// this should happen
		}
	}
	
	public void testDoubleDeclaration() {
		try {
			ts.declareAnnotation(E, "size", tf.integerType());
		}
		catch (FactTypeDeclarationException e) {
			fail(e.toString());
		}
		catch (FactTypeUseException e) {
			fail(e.toString());
		}
		
		try {
			ts.declareAnnotation(E, "size", tf.realType());
			fail("double declaration is not allowed");
		}
		catch (FactTypeDeclarationException e) {
			// this should happen
		}
	}
	
	public void testSetAnnotation() {
		IConstructor n = (IConstructor) N.make(vf, vf.integer(0));
		ts.declareAnnotation(E, "size", tf.integerType());
		
		try {
			n.setAnnotation("size", vf.integer(0));
		}
		catch (FactTypeDeclarationException e) {
			fail(e.toString());
		}
		catch (FactTypeUseException e) {
			fail(e.toString());
		}
	}
	
	public void testGetAnnotation() {
		IConstructor n = (IConstructor) N.make(vf, vf.integer(0));
		ts.declareAnnotation(E, "size", tf.integerType());
		
		try {
			if (n.getAnnotation("size") != null) {
				fail("annotation should be null");
			}
		} catch (FactTypeUseException e) {
			fail(e.toString());
		}
		
		IConstructor m = n.setAnnotation("size", vf.integer(1));
		IValue b = m.getAnnotation("size");
		if (!b.isEqual(vf.integer(1))) {
			fail();
		}
	}
	
	public void testImmutability() {
		IConstructor n = (IConstructor) N.make(vf, vf.integer(0));
		ts.declareAnnotation(E, "size", tf.integerType());
		
		IConstructor m = n.setAnnotation("size", vf.integer(1));
		
		if (m == n) {
			fail("annotation setting should change object identity");
		}
		
		assertTrue(m.isEqual(n));
	}
	
	public void testDeclaresAnnotation() {
		IConstructor n = (IConstructor) N.make(vf, vf.integer(0));
		ts.declareAnnotation(E, "size", tf.integerType());
		
		if (!n.declaresAnnotation(ts, "size")) {
			fail();
		}
		
		if (n.declaresAnnotation(ts, "size2")) {
			fail();
		}
	}
	
	public void testEqualityNode() {
		INode n = vf.node("hello");
		INode na = n.setAnnotation("audience", vf.string("world"));
		
		assertTrue(n.isEqual(na));
		assertTrue(vf.set(n).isEqual(vf.set(na)));
		assertTrue(vf.list(n).isEqual(vf.list(na)));
		assertTrue(vf.set(vf.set(n)).isEqual(vf.set(vf.set(na))));
	}
	
	public void testEqualityConstructor() {
		IConstructor n = (IConstructor) N.make(vf, vf.integer(1));
		IConstructor na = n.setAnnotation("x", vf.integer(1));
		
		assertTrue(n.isEqual(na));
		assertTrue(vf.set(n).isEqual(vf.set(na)));
		assertTrue(vf.list(n).isEqual(vf.list(na)));
		assertTrue(vf.set(vf.set(n)).isEqual(vf.set(vf.set(na))));
	}
	
	public void testNodeAnnotation() {
		ts.declareAnnotation(tf.nodeType(), "foo", tf.boolType());
		INode n = vf.node("hello");
		INode na = n.setAnnotation("foo", vf.bool(true));
		
		assertTrue(na.getAnnotation("foo").getType().isBoolType());
		
		// annotations on node type should be propagated
		assertTrue(ts.getAnnotationType(tf.nodeType(), "foo").isBoolType());
		assertTrue(ts.getAnnotations(E).containsKey("foo"));
		
		// annotations sets should not collapse into one big set
		ts.declareAnnotation(E, "a", tf.integerType());
		ts.declareAnnotation(N, "b", tf.boolType());
		assertTrue(!ts.getAnnotations(E).equals(ts.getAnnotations(N)));
	}
}
