/*******************************************************************************
 * Copyright (c) 2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package org.eclipse.imp.pdb.test.persistent;

import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.persistent.ValueFactory;
import org.eclipse.imp.pdb.facts.util.ImmutableSet;
import org.eclipse.imp.pdb.facts.util.TrieSet;
import org.junit.BeforeClass;
import org.junit.Test;

public class TrieSetTests {

	private class DummyValue {
		public final int value;
		public final int hashCode;
		
		DummyValue(int value, int hashCode) {
			this.value = value;
			this.hashCode = hashCode; 
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DummyValue other = (DummyValue) obj;
			if (hashCode != other.hashCode)
				return false;
			if (value != other.value)
				return false;
			return true;
		}	
		
		@Override
		public String toString() {
			return String.format("%d [hashCode=%d]", value, hashCode);
		}
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Test
	public void testNodeValNode() {
		Map<Integer, Integer> input = new LinkedHashMap<>();
		
		input.put(1, 1);
		input.put(2, 33);
		input.put(3, 3);
		input.put(4, 4);
		input.put(5, 4);
		input.put(6, 6);
		input.put(7, 7);
		input.put(8, 7);
		
		ImmutableSet<DummyValue> set = TrieSet.of();
		
		for (Entry<Integer, Integer> entry : input.entrySet()) {
			set = set.__insert(new DummyValue(entry.getKey(), entry.getValue()));	
		}
				
		for (Entry<Integer, Integer> entry : input.entrySet()) {
			assertTrue(set.contains(new DummyValue(entry.getKey(), entry.getValue())));
		}
	}

	@Test
	public void testValNodeVal() {
		Map<Integer, Integer> input = new LinkedHashMap<>();
		
		input.put(1, 1);
		input.put(2, 2);
		input.put(3, 2);
		input.put(4, 4);
		input.put(5, 5);
		input.put(6, 5);
		input.put(7, 7);
		
		ImmutableSet<DummyValue> set = TrieSet.of();
		
		for (Entry<Integer, Integer> entry : input.entrySet()) {
			set = set.__insert(new DummyValue(entry.getKey(), entry.getValue()));	
		}
				
		for (Entry<Integer, Integer> entry : input.entrySet()) {
			assertTrue(set.contains(new DummyValue(entry.getKey(), entry.getValue())));
		}	
	}
	
	@Test
	public void testIteration() {
		Map<Integer, Integer> input = new LinkedHashMap<>();
		
		input.put(1, 1);
		input.put(2, 2);
		input.put(3, 2);
		input.put(4, 4);
		input.put(5, 5);
		input.put(6, 5);
		input.put(7, 7);
		
		ImmutableSet<DummyValue> set = TrieSet.of();
		
		for (Entry<Integer, Integer> entry : input.entrySet()) {
			set = set.__insert(new DummyValue(entry.getKey(), entry.getValue()));	
		}
		
		Set<Integer> keys = input.keySet();
		
		for (DummyValue key : set) {
			keys.remove(key.value);
		}
		
		assertTrue (keys.isEmpty());
	}
	
	@Test
	public void testExtendedIteration() {
		IValueFactory valueFactory = ValueFactory.getInstance();
		int size = 10_000;
		
		ISetWriter writer = valueFactory.setWriter();
		
		Random random = new Random();
		for (int i = size; i > 0; i--) {
//			writer.insert(valueFactory.integer(i));
			
			// Random			
			writer.insert(valueFactory.integer(random.nextInt()));
		}
		
		ISet testSet = writer.done();
		int realSize = testSet.size();

		int countedSize = 0;
		for (Object key : testSet) {
			countedSize++;
		}
		
		System.out.println(String.format("realSize[%d] == countedSize[%d]", realSize, countedSize));
		assertTrue (realSize == countedSize);
	}
	
	@Test
	public void testEqualityAfterInsertDelete() {
		IValueFactory valueFactory = ValueFactory.getInstance();
		int size = 50;
				
		ISetWriter writer1 = valueFactory.setWriter();
		ISetWriter writer2 = valueFactory.setWriter();
		
		for (int i = size; i > 0; i--) {
			writer1.insert(valueFactory.integer(i));
			writer2.insert(valueFactory.integer(i));
		}
		
		ISet testSet = writer1.done();
		ISet testSetDuplicate = writer2.done();
	
//		IValue VALUE_EXISTING = valueFactory.integer(size - 1);
		IValue VALUE_NOT_EXISTING = valueFactory.integer(size + 1);

		testSetDuplicate = testSet.insert(VALUE_NOT_EXISTING);
		testSetDuplicate = testSetDuplicate.delete(VALUE_NOT_EXISTING);
		
		boolean equals = testSet.equals(testSetDuplicate);
		
		assertTrue (equals);
	}
	
}
