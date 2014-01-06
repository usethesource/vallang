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

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
	
}
