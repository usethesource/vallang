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
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;

public abstract class BaseTestList extends TestCase {
    private IValueFactory vf;
    private TypeFactory tf = TypeFactory.getInstance();
    
    private IValue[] integers;
    private IList integerList;
    
	protected void setUp(IValueFactory factory) throws Exception {
		super.setUp();
		vf = factory;
		
		integers = new IValue[20];
		IListWriter w = vf.listWriter(tf.integerType());
		
		for (int i = 0; i < integers.length; i++) {
			integers[i] = vf.integer(i);
		}
		
		for (int i = integers.length - 1; i >= 0; i--) {
			w.insert(vf.integer(i));
		}
		
		integerList = w.done();
	}

	public void testGetElementType() {
		if (integerList.getElementType() != tf.integerType()) {
			fail("funny getElementType");
		}
		
		try {
			IList namedList = (IList) tf.aliasType(new TypeStore(), "myList", tf.listType(tf.integerType())).make(vf);
			if (namedList.getElementType() != tf.integerType()) {
				fail("named list has wrong elementtype");
			}
		} catch (FactTypeUseException e) {
			fail("the above should be type correct");
		}
		
	}

	public void testAppend() {
		try {
			IValue newValue = vf.integer(integers.length);
			IList longer = integerList.append(newValue);
			
			if (longer.length() != integerList.length() + 1) {
				fail("append failed");
			}
			
			if (!longer.get(integerList.length()).isEqual(newValue)) {
				fail("element was not appended");
			}
			
		} catch (FactTypeUseException e) {
			fail("the above should be type correct");
		}
		
		try {
			if (!integerList.append(vf.real(2)).getElementType().isNumberType()) {
			  fail("append should lub the element type");
			}
		} catch (FactTypeUseException e) {
			// this should happen
		}
	}

	public void testGet() {
		for (int i = 0; i < integers.length; i++) {
			if (!integerList.get(i).isEqual(integers[i])) {
				fail("get failed");
			}
		}
	}

	public void testInsert() {
		try {
			IValue newValue = vf.integer(integers.length);
			IList longer = integerList.insert(newValue);
			
			if (longer.length() != integerList.length() + 1) {
				fail("append failed");
			}
			
			if (!longer.get(0).isEqual(newValue)) {
				fail("element was not insrrted");
			}
			
		} catch (FactTypeUseException e) {
			fail("the above should be type correct");
		}
		
		try {
			if (!integerList.insert(vf.real(2)).getElementType().isNumberType()) {
			  fail("insert should lub the element type");
			}
		} catch (FactTypeUseException e) {
			// this should happen
		}
	}

	public void testLength() {
		if (vf.list(tf.integerType()).length() != 0) {
			fail("empty list should be size 0");
		}
		
		if (integerList.length() != integers.length) {
			fail("length does not count amount of elements");
		}
	}

	public void testReverse() {
		IList reverse = integerList.reverse();
		
		if (reverse.getType() != integerList.getType()) {
			fail("reverse should keep type");
		}
		
		if (reverse.length() != integerList.length()) {
			fail("length of reverse is different");
		}
		
		for (int i = 0; i < integers.length; i++) {
			if (!reverse.get(i).isEqual(integers[integers.length - i - 1])) {
				fail("reverse did something funny: " + reverse + " is not reverse of " + integerList);
			}
		}
	}

	public void testIterator() {
		Iterator<IValue> it = integerList.iterator();
		
		int i;
		for (i = 0; it.hasNext(); i++) {
			IValue v = it.next();
			if (!v.isEqual(integers[i])) {
				fail("iterator does not iterate in order");
			}
		}
	}
	
	// NOTE: This is not a very good test, but sufficient for it's purpose.
	public void testSubList(){
		// Front
		IListWriter flw = vf.listWriter(tf.integerType());
		for(int i = 0; i < 20; i++){
			flw.append(vf.integer(i));
		}
		IList fList = flw.done();
		
		// Back
		IListWriter blw = vf.listWriter(tf.integerType());
		for(int i = 19; i >= 0; i--){
			blw.insert(vf.integer(i));
		}
		IList bList = blw.done();
		
		// Overlap
		IListWriter olw = vf.listWriter(tf.integerType());
		for(int i = 9; i >= 0; i--){
			olw.insert(vf.integer(i));
		}
		for(int i = 10; i < 20; i++){
			olw.append(vf.integer(i));
		}
		IList oList = olw.done();
		
		IList fSubList = fList.sublist(0, 5);
		IList bSubList = bList.sublist(0, 5);
		IList oSubList = oList.sublist(0, 5);
		checkSubListEquality(fSubList, bSubList, oSubList);
		
		fSubList = fList.sublist(1, 5);
		bSubList = bList.sublist(1, 5);
		oSubList = oList.sublist(1, 5);
		checkSubListEquality(fSubList, bSubList, oSubList);
		
		fSubList = fList.sublist(0, 15);
		bSubList = bList.sublist(0, 15);
		oSubList = oList.sublist(0, 15);
		checkSubListEquality(fSubList, bSubList, oSubList);
		
		fSubList = fList.sublist(1, 15);
		bSubList = bList.sublist(1, 15);
		oSubList = oList.sublist(1, 15);
		checkSubListEquality(fSubList, bSubList, oSubList);
		
		fSubList = fList.sublist(5, 5);
		bSubList = bList.sublist(5, 5);
		oSubList = oList.sublist(5, 5);
		checkSubListEquality(fSubList, bSubList, oSubList);
		
		fSubList = fList.sublist(5, 10);
		bSubList = bList.sublist(5, 10);
		oSubList = oList.sublist(5, 10);
		checkSubListEquality(fSubList, bSubList, oSubList);
		
		fSubList = fList.sublist(15, 5);
		bSubList = bList.sublist(15, 5);
		oSubList = oList.sublist(15, 5);
		checkSubListEquality(fSubList, bSubList, oSubList);
	}
	
	private static void checkSubListEquality(IList fList, IList bList, IList oList){
		if(!fList.isEqual(bList) || !bList.isEqual(oList)) fail("IList#subList is broken: "+fList+" "+bList+" "+oList);
	}
}
