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

import java.util.Collection;

import org.eclipse.imp.pdb.facts.util.ImmutableMultimap;
import org.eclipse.imp.pdb.facts.util.TrieMultimap_BleedingEdge;
import org.junit.Test;

public class TrieMultimapTests {
	
	final static int size = 64;
	
	@Test
	public void testInsertTwoTuplesThatShareSameKey() {
		ImmutableMultimap<Integer, String> map = TrieMultimap_BleedingEdge.<Integer, String>of()
						.__put(1, "x")
						.__put(1, "y");					
		
		assertEquals(2, map.size());
		assertTrue(map.containsKey(1));
	}

	@Test
	public void testInsertTwoTuplesWithOneRemoveThatShareSameKeyX() {
		ImmutableMultimap<Integer, String> map = TrieMultimap_BleedingEdge.<Integer, String>of()
						.__put(1, "x")
						.__put(1, "y")
						.__remove(1, "x");					
		
		assertEquals(1, map.size());
		assertTrue(map.containsKey(1));
	}

	@Test
	public void testInsertTwoTuplesWithOneRemoveThatShareSameKeyY() {
		ImmutableMultimap<Integer, String> map = TrieMultimap_BleedingEdge.<Integer, String>of()
						.__put(1, "x")
						.__put(1, "y")
						.__remove(1, "y");
		
		assertEquals(1, map.size());
		assertTrue(map.containsKey(1));
	}

	@Test
	public void testInsertTwoTuplesWithOneRemoveThatShareSameKeyXY() {
		ImmutableMultimap<Integer, String> map = TrieMultimap_BleedingEdge.<Integer, String>of()
						.__put(1, "x")
						.__put(1, "y")
						.__remove(1, "x")
						.__remove(1, "y");
		
		assertEquals(0, map.size());
		assertFalse(map.containsKey(1));
	}

	@Test
	public void testInsertTwoTuplesThatShareSameKey_Iterate() {
		ImmutableMultimap<Integer, String> map = TrieMultimap_BleedingEdge.<Integer, String>of()
						.__put(1, "x")
						.__put(1, "y");					
		
		Collection<String> values = map.values();
		
		assertEquals(2, values.size());
		assertTrue(values.contains("x"));
		assertTrue(values.contains("y"));
	}
	
}
