package org.eclipse.imp.pdb.test.persistent;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;
import static org.eclipse.imp.pdb.facts.util.SquashedHashCodeUtils.*;

public class SquashedHashCodes {

	@Test
	public void testGetSquasedHashes() {
		int bh0 = squashHash(3093445);
		int bh1 = squashHash(-230934599);
		int bh2 = squashHash(120000009);
		int bh3 = squashHash(-1);
		
		int hashes = combineFourSquashedHashes(bh0, bh1, bh2, bh3);
		
		assertEquals(bh0, getSquashedHash(hashes, 0));
		assertEquals(bh1, getSquashedHash(hashes, 1));
		assertEquals(bh2, getSquashedHash(hashes, 2));
		assertEquals(bh3, getSquashedHash(hashes, 3));		
	}	
	
	@Test
	public void testGetSquasedHashesLong() {
		int bh0 = squashHash(3093445);
		int bh1 = squashHash(-230934599);
		int bh2 = squashHash(120000009);
		int bh3 = squashHash(-1);
		
		int hashes = combineFourSquashedHashes(bh0, bh1, bh2, bh3);
		
		assertEquals(bh0, getSquashedHash(hashes, 0));
		assertEquals(bh1, getSquashedHash(hashes, 1));
		assertEquals(bh2, getSquashedHash(hashes, 2));
		assertEquals(bh3, getSquashedHash(hashes, 3));		
	}		
	
	@Test
	public void testInsertFront() {
		int bh = 0xFF;
		int idx = 0;
		
		int[] outputs = arraycopyAndInsertInt(0, 0, 0, 0, 0, 0, 0, 0, idx, bh);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(bh << 24, outputs[0]);
		assertEquals(0, outputs[1]);
		assertEquals(0, outputs[2]);
		assertEquals(0, outputs[3]);
		assertEquals(0, outputs[4]);
		assertEquals(0, outputs[5]);
		assertEquals(0, outputs[6]);
		assertEquals(0, outputs[7]);
	}
	
	@Test
	public void testInsertFrontOverflow() {
		int bh = 0x00;
		int idx = 0;
		
		int[] outputs = arraycopyAndInsertInt(-1, 0, -1, -1, -1, -1, -1, -1, idx, bh);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0x00FFFFFF, outputs[0]);
		assertEquals(0xFF000000, outputs[1]);
		assertEquals(0x00FFFFFF, outputs[2]);
		assertEquals(0xFFFFFFFF, outputs[3]);
		assertEquals(0xFFFFFFFF, outputs[4]);
		assertEquals(0xFFFFFFFF, outputs[5]);
		assertEquals(0xFFFFFFFF, outputs[6]);
		assertEquals(0xFFFFFFFF, outputs[7]);
		
	}	

	@Test
	public void testInsertBack() {
		int bh = 0xFF;
		int idx = 3;
		
		int[] outputs = arraycopyAndInsertInt(0, 0, 0, 0, 0, 0, 0, 0, idx, bh);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(bh << 0, outputs[0]);
		assertEquals(0, outputs[1]);
		assertEquals(0, outputs[2]);
		assertEquals(0, outputs[3]);
		assertEquals(0, outputs[4]);
		assertEquals(0, outputs[5]);
		assertEquals(0, outputs[6]);
		assertEquals(0, outputs[7]);		
	}
		
	@Test
	public void testInsertBackOverflow() {
		int bh = 0x00;
		int idx = 3;
		
		int[] outputs = arraycopyAndInsertInt(-1, 0, -1, -1, -1, -1, -1, -1, idx, bh);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0xFFFFFF00, outputs[0]);
		assertEquals(0xFF000000, outputs[1]);
		assertEquals(0x00FFFFFF, outputs[2]);
		assertEquals(0xFFFFFFFF, outputs[3]);
		assertEquals(0xFFFFFFFF, outputs[4]);
		assertEquals(0xFFFFFFFF, outputs[5]);
		assertEquals(0xFFFFFFFF, outputs[6]);
		assertEquals(0xFFFFFFFF, outputs[7]);		
	} 
	
	@Test
	public void testRemoveFrontOverflow1() {
		int idx = 0;
		
		int[] outputs = arraycopyAndRemoveInt(0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00, idx);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0x00FF00FF, outputs[0]);
		assertEquals(0x00FF00FF, outputs[1]);
		assertEquals(0x00FF00FF, outputs[2]);
		assertEquals(0x00FF00FF, outputs[3]);
		assertEquals(0x00FF00FF, outputs[4]);
		assertEquals(0x00FF00FF, outputs[5]);
		assertEquals(0x00FF00FF, outputs[6]);		
		assertEquals(0x00FF0000, outputs[7]);
	}	
	
	@Test
	public void testRemoveFrontOverflow2() {
		int idx = 0;
		
		int[] outputs = arraycopyAndRemoveInt(0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, idx);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0xFF00FF00, outputs[0]);
		assertEquals(0xFF00FF00, outputs[1]);
		assertEquals(0xFF00FF00, outputs[2]);
		assertEquals(0xFF00FF00, outputs[3]);
		assertEquals(0xFF00FF00, outputs[4]);
		assertEquals(0xFF00FF00, outputs[5]);
		assertEquals(0xFF00FF00, outputs[6]);
		assertEquals(0xFF00FF00, outputs[7]);		
	}
	
	@Test
	public void testRemoveFrontOverflow3() {
		int idx = 2;
		
		int[] outputs = arraycopyAndRemoveInt(0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, idx);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0x00FFFF00, outputs[0]);
		assertEquals(0xFF00FF00, outputs[1]);
		assertEquals(0xFF00FF00, outputs[2]);
		assertEquals(0xFF00FF00, outputs[3]);
		assertEquals(0xFF00FF00, outputs[4]);
		assertEquals(0xFF00FF00, outputs[5]);
		assertEquals(0xFF00FF00, outputs[6]);
		assertEquals(0xFF00FF00, outputs[7]);		
	}		
	
	@Test
	public void testRemoveFrontOverflow4() {
		int idx = 6;
		
		int[] outputs = arraycopyAndRemoveInt(0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, idx);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0x00FF00FF, outputs[0]);
		assertEquals(0x00FFFF00, outputs[1]);
		assertEquals(0xFF00FF00, outputs[2]);
		assertEquals(0xFF00FF00, outputs[3]);
		assertEquals(0xFF00FF00, outputs[4]);
		assertEquals(0xFF00FF00, outputs[5]);
		assertEquals(0xFF00FF00, outputs[6]);
		assertEquals(0xFF00FF00, outputs[7]);		
	}		
	
}
