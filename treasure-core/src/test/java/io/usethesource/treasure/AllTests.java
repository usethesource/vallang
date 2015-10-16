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

package io.usethesource.treasure;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	// TODO: this test suite tests the basic functionality of sets, relations and lists;
	// it also checks the functionality of the type factory and the computation of 
	// the least upperbound of types and the isSubtypeOf method. It needs more tests
	// for named types and the way they are checked and produced by the implementations
	// of IRelation, ISet and IList.
	
	public static Test suite() {
		TestSuite suite = new TestSuite(
				"Test for org.eclipse.imp.pdb");
	
		suite.addTestSuite(TestType.class);
		suite.addTestSuite(TestTypeFactory.class);
		suite.addTestSuite(TestIO.class);
		suite.addTestSuite(TestBinaryIO.class);

		addReferenceTests(suite);
		addFastTests(suite);
		addPersistentTests(suite);
		
		return suite;
	}

	private static void addReferenceTests(TestSuite suite) {
		suite.addTestSuite(io.usethesource.treasure.reference.TestAnnotations.class);
		suite.addTestSuite(io.usethesource.treasure.reference.TestBasicValues.class);
		suite.addTestSuite(io.usethesource.treasure.reference.TestEquality.class);
		suite.addTestSuite(io.usethesource.treasure.reference.TestList.class);
		suite.addTestSuite(io.usethesource.treasure.reference.TestListRelation.class);
		suite.addTestSuite(io.usethesource.treasure.reference.TestMap.class);
		suite.addTestSuite(io.usethesource.treasure.reference.TestRandomValues.class);
		suite.addTestSuite(io.usethesource.treasure.reference.TestRelation.class);
		suite.addTestSuite(io.usethesource.treasure.reference.TestSet.class);
		suite.addTestSuite(io.usethesource.treasure.reference.TestValueFactory.class);
	}

	private static void addFastTests(TestSuite suite) {
		suite.addTestSuite(io.usethesource.treasure.fast.TestAnnotations.class);
		suite.addTestSuite(io.usethesource.treasure.fast.TestBasicValues.class);
		suite.addTestSuite(io.usethesource.treasure.fast.TestEquality.class);
		suite.addTestSuite(io.usethesource.treasure.fast.TestList.class);
		suite.addTestSuite(io.usethesource.treasure.fast.TestListRelation.class);
		suite.addTestSuite(io.usethesource.treasure.fast.TestMap.class);
		suite.addTestSuite(io.usethesource.treasure.fast.TestRandomValues.class);
		suite.addTestSuite(io.usethesource.treasure.fast.TestRelation.class);
		suite.addTestSuite(io.usethesource.treasure.fast.TestSet.class);
		suite.addTestSuite(io.usethesource.treasure.fast.TestValueFactory.class);
	}
	
	private static void addPersistentTests(TestSuite suite) {
		suite.addTestSuite(io.usethesource.treasure.persistent.TestAnnotations.class);
		suite.addTestSuite(io.usethesource.treasure.persistent.TestBasicValues.class);
		suite.addTestSuite(io.usethesource.treasure.persistent.TestEquality.class);
		suite.addTestSuite(io.usethesource.treasure.persistent.TestList.class);
		suite.addTestSuite(io.usethesource.treasure.persistent.TestListRelation.class);
		suite.addTestSuite(io.usethesource.treasure.persistent.TestMap.class);
		suite.addTestSuite(io.usethesource.treasure.persistent.TestRandomValues.class);
		suite.addTestSuite(io.usethesource.treasure.persistent.TestRelation.class);
		suite.addTestSuite(io.usethesource.treasure.persistent.TestSet.class);
		suite.addTestSuite(io.usethesource.treasure.persistent.TestValueFactory.class);
	}
	
}
