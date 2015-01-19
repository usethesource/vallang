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

import java.util.Random;

import org.eclipse.imp.pdb.facts.util.TrieMap_5Bits;
import org.junit.Test;

public class TrieMapTests {
	
	final static int size = 64;
	
	@Test
	public void testPrintStatsSequential() {
//		int size = 128;

		TrieMap_5Bits<Integer, Integer> map = (TrieMap_5Bits) TrieMap_5Bits.of();
		
		for (int i = size; i > 0; i--) {
			TrieMap_5Bits<Integer, Integer> res = (TrieMap_5Bits) map.__put(i, i);
			assert res.containsKey(i);			
			map = res;
		}
		
		map.printStatistics();
	}
	
	@Test
	public void testPrintStatsRandom() {
//		int size = 128;

		TrieMap_5Bits<Integer, Integer> map = (TrieMap_5Bits) TrieMap_5Bits.of();
		
		Random rand = new Random(13);
		
		for (int i = size; i > 0; i--) {
			final int j = rand.nextInt(); 
			
			TrieMap_5Bits<Integer, Integer> res = (TrieMap_5Bits) map.__put(j, j);
			assert res.containsKey(j);			
			map = res;
		}
		
		map.printStatistics();
	}
		
	@Test
	public void testCheckPrefixConstruction() {
//		int size = 128;

		TrieMap_5Bits<Integer, Integer> map = (TrieMap_5Bits) TrieMap_5Bits.of();
		
		TrieMap_5Bits<Integer, Integer> res1 = (TrieMap_5Bits) map
						.__put(63, 63)
						.__put(64, 64)					
						.__put(32768, 32768)
						.__put(2147483647, 2147483647)
						.__put(65536, 65536);
		
		assert res1.containsKey(63);
		assert res1.containsKey(64);
		assert res1.containsKey(32768);
		assert res1.containsKey(65536);
		assert res1.containsKey(2147483647);

		TrieMap_5Bits<Integer, Integer> res2 = (TrieMap_5Bits) map
						.__put(2147483647, 2147483647)
						.__put(32768, 32768)
						.__put(63, 63)
						.__put(64, 64)
						.__put(65536, 65536);		

		assert res2.containsKey(63);
		assert res2.containsKey(64);
		assert res2.containsKey(32768);
		assert res2.containsKey(65536);
		assert res2.containsKey(2147483647);
		
		assert res1.equals(res2);
		
		map.printStatistics();
	}	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testCheckCompactionFromBeginUponDelete() {

		TrieMap_5Bits<Integer, Integer> map = (TrieMap_5Bits) TrieMap_5Bits.of();
		
		TrieMap_5Bits<Integer, Integer> res1 = (TrieMap_5Bits) map
						.__put(1, 1)
						.__put(2, 2);					

		TrieMap_5Bits<Integer, Integer> res2 = (TrieMap_5Bits) res1
						.__put(32769, 32769)
						.__remove(2);		

		// what to test for?
		assert !res1.equals(res2);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testCheckCompactionFromMiddleUponDelete() {

		TrieMap_5Bits<Integer, Integer> map = (TrieMap_5Bits) TrieMap_5Bits.of();
		
		TrieMap_5Bits<Integer, Integer> res1 = (TrieMap_5Bits) map
						.__put(1, 1)
						.__put(2, 2)
						.__put(65, 65)
						.__put(66, 66);					

		TrieMap_5Bits<Integer, Integer> res2 = (TrieMap_5Bits) res1
						.__put(32769, 32769)
						.__remove(66);		

		// what to test for?
		assert !res1.equals(res2);
	}	
	
	@Test
	public void testRecoverMask() {
		byte mask = recoverMask(-2147483648, (byte) 1);
		assertTrue(mask == 31);
	}
	
	static byte recoverMask(int map, byte i_th) {
		assert 1 <= i_th && i_th <= 32;
		
		byte cnt1 = 0;
		byte mask = 0;
		
		while(mask < 32) {
			if ((map & 0x01) == 0x01) {
				cnt1 += 1;
				
				if (cnt1 == i_th) {
					return mask;
				}
			}
			
			map = map >> 1;
			mask += 1;
		}
		
		throw new RuntimeException("Called with invalid arguments."); // cnt1 != i_th
	}	
	
}
