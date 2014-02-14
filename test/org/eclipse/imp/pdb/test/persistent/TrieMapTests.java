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

import java.util.Random;

import org.eclipse.imp.pdb.facts.util.TrieMap;
import org.junit.Test;

public class TrieMapTests {
	
	final static int size = 64;
	
	@Test
	public void testPrintStatsSequential() {
//		int size = 128;

		TrieMap<Integer, Integer> map = (TrieMap) TrieMap.of();
		
		for (int i = size; i > 0; i--) {
			map = map.__put(i, i);
		}
		
		map.printStats();
	}
	
	@Test
	public void testPrintStatsRandom() {
//		int size = 128;

		TrieMap<Integer, Integer> map = (TrieMap) TrieMap.of();
		
		Random rand = new Random();
		
		for (int i = size; i > 0; i--) {
			final int j = rand.nextInt(); 
			
			map = map.__put(j, j);
		}
		
		map.printStats();
	}
	
}
