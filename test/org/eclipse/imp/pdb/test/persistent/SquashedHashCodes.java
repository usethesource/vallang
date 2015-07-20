package org.eclipse.imp.pdb.test.persistent;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

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
	public void testInsertFront() {
		int bh = 0xFF;
		int idx = 0;
		
		int[] outputs = arraycopyAndInsertInt(0, 0, 0, 0, idx, bh);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(bh << 24, outputs[0]);
		assertEquals(0, outputs[1]);
		assertEquals(0, outputs[2]);
		assertEquals(0, outputs[3]);
	}
	
	@Test
	public void testInsertFrontOverflow() {
		int bh = 0x00;
		int idx = 0;
		
		int[] outputs = arraycopyAndInsertInt(-1, 0, -1, -1, idx, bh);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0x00FFFFFF, outputs[0]);
		assertEquals(0xFF000000, outputs[1]);
		assertEquals(0x00FFFFFF, outputs[2]);
		assertEquals(0xFFFFFFFF, outputs[3]);
	}	

	@Test
	public void testInsertBack() {
		int bh = 0xFF;
		int idx = 3;
		
		int[] outputs = arraycopyAndInsertInt(0, 0, 0, 0, idx, bh);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(bh << 0, outputs[0]);
		assertEquals(0, outputs[1]);
		assertEquals(0, outputs[2]);
		assertEquals(0, outputs[3]);
	}
		
	@Test
	public void testInsertBackOverflow() {
		int bh = 0x00;
		int idx = 3;
		
		int[] outputs = arraycopyAndInsertInt(-1, 0, -1, -1, idx, bh);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0xFFFFFF00, outputs[0]);
		assertEquals(0xFF000000, outputs[1]);
		assertEquals(0x00FFFFFF, outputs[2]);
		assertEquals(0xFFFFFFFF, outputs[3]);
	}		
	
	static final int squashHash(final int h) {
		return (h ^ h >>> 20) & 0xFF;
	}
	
	static final int getSquashedHash(final int byteHashes, int pos) {
		return (byteHashes >>> (24 - pos * 8)) & 0xFF;
	}

	static final int shiftSquashedHash(final int squashedHash, int pos) {
		assert squashedHash == (squashedHash & 0xFF);
		return (squashedHash & 0xFF) << (24 - pos * 8);
	}
	
	static final int combineFourSquashedHashes(int h0, int h1, int h2, int h3) {
		final int r0 = h0 << 24;
		final int r1 = h1 << 16;
		final int r2 = h2 <<  8;
		final int r3 = h3;
		return r0 ^ r1 ^ r2 ^ r3;
	}

	static int[] arraycopyAndInsertInt(int input0, int input1, int input2, int input3, int idx, final int squashedHash) {
		// final int[] maskOfHash = new int[] { 0xFF000000, 0xFF0000, 0xFF00, 0xFF };
		final int[] maskOfHashLR = new int[] { 0xFF000000, 0xFFFF0000, 0xFFFFFF00, 0xFFFFFFFF };
		final int[] maskOfHashRL = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x0000FFFF, 0x000000FF };

		final int[] inputs = new int[] { input0, input1, input2, input3 };
		final int[] outputs = new int[4];
		
		// COPY SEGMENTS BEFORE	
		int segment = 0;
		for (; segment < 4 && idx > 4; segment++) {
			outputs[segment] = inputs[segment];
			idx -= 4;
		}

		// INSERT INTO SEGMENT	
		final int left;
		if (idx == 0) {
			left = 0;
		} else {
			left = inputs[segment] & maskOfHashLR[idx - 1];
		}
			
		final int middle = shiftSquashedHash(squashedHash, idx);
		
		final int right;
		if (idx == 3) {
			right = 0;
		} else {
			right = (inputs[segment] >>> 8) & maskOfHashRL[idx + 1];
		}
		
		outputs[segment] = left | middle | right;
		int remainder = inputs[segment] & 0xFF;
		segment++;
		
		// SHIFT AND COPY AFTER
		for (; segment < 4; segment++) {
			outputs[segment] = (remainder << 24) ^ (inputs[segment] >>> 8);
			remainder = inputs[segment] & 0xFF;
		}
			
		return outputs;
	}	
	
	@Test
	public void testRemoveFrontOverflow1() {
		int idx = 0;
		
		int[] outputs = arraycopyAndRemoveInt(0xFF00FF00, 0xFF00FF00, 0xFF00FF00, 0xFF00FF00, idx);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0x00FF00FF, outputs[0]);
		assertEquals(0x00FF00FF, outputs[1]);
		assertEquals(0x00FF00FF, outputs[2]);
		assertEquals(0x00FF0000, outputs[3]);
	}	
	
	@Test
	public void testRemoveFrontOverflow2() {
		int idx = 0;
		
		int[] outputs = arraycopyAndRemoveInt(0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, idx);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0xFF00FF00, outputs[0]);
		assertEquals(0xFF00FF00, outputs[1]);
		assertEquals(0xFF00FF00, outputs[2]);
		assertEquals(0xFF00FF00, outputs[3]);
	}
	
	@Test
	public void testRemoveFrontOverflow3() {
		int idx = 2;
		
		int[] outputs = arraycopyAndRemoveInt(0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, idx);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0x00FFFF00, outputs[0]);
		assertEquals(0xFF00FF00, outputs[1]);
		assertEquals(0xFF00FF00, outputs[2]);
		assertEquals(0xFF00FF00, outputs[3]);
	}		
	
	@Test
	public void testRemoveFrontOverflow4() {
		int idx = 6;
		
		int[] outputs = arraycopyAndRemoveInt(0x00FF00FF, 0x00FF00FF, 0x00FF00FF, 0x00FF00FF, idx);
		
		System.out.println(Arrays.toString(outputs));
		
		assertEquals(0x00FF00FF, outputs[0]);
		assertEquals(0x00FFFF00, outputs[1]);
		assertEquals(0xFF00FF00, outputs[2]);
		assertEquals(0xFF00FF00, outputs[3]);
	}		
	
	static int[] arraycopyAndRemoveInt(int input0, int input1, int input2, int input3, int idx) {
		// final int[] maskOfHash = new int[] { 0xFF000000, 0xFF0000, 0xFF00, 0xFF };
		final int[] maskOfHashLR = new int[] { 0xFF000000, 0xFFFF0000, 0xFFFFFF00, 0xFFFFFFFF };
		final int[] maskOfHashRL = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x0000FFFF, 0x000000FF };

		final int[] inputs = new int[] { input0, input1, input2, input3 };
		final int[] outputs = new int[4];
		
		int segment = 0;
		
		// COPY SEGMENTS BEFORE	
		for (; segment < 4 && idx > 4; segment++) {
			outputs[segment] = inputs[segment];
			idx -= 4;
		}

		// REMOVE FROM SEGMENT	
		final int left;
		if (idx == 0) {
			left = 0;
		} else {
			left = inputs[segment] & maskOfHashLR[idx - 1];
		}
		
		final int middle;
		if (idx == 3) {
			middle = 0;
		} else {
			middle = (inputs[segment] << 8) & maskOfHashRL[idx];
		}
		
		final int right;
		if (segment >= 3) {
			right = 0;
		} else {
			right = inputs[segment + 1] >>> 24;
		}		
		
		outputs[segment] = left | middle | right;
		segment++;
		
		// SHIFT AND COPY AFTER
		for (; segment < 4; segment++) {
			final int remainder;
			if (segment >= 3) {
				remainder = 0;
			} else {
				remainder = inputs[segment + 1] >>> 24;
			}		
			
			outputs[segment] = (inputs[segment] << 8) ^ remainder;
		}
			
		return outputs;
	}
		
}
