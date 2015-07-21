package org.eclipse.imp.pdb.facts.util;

public class SquashedHashCodeUtils {

	public static final int squashHash(final int h) {
		return (h ^ h >>> 20) & 0xFF;
	}
	
	public static final int getSquashedHash(final int byteHashes0, final int byteHashes1,
			final int byteHashes2, final int byteHashes3, final int byteHashes4,
			final int byteHashes5, final int byteHashes6, final int byteHashes7, int pos) {
		final int byteHashes;
		
		if (pos < 16) {
			if (pos < 8) {
				if (pos < 4) {
					byteHashes = byteHashes0;
				} else {
					byteHashes = byteHashes1;
				}
			} else {
				if (pos < 12) {
					byteHashes = byteHashes2;
				} else {
					byteHashes = byteHashes3;
				}
			}
		} else {
			if (pos < 24) {
				if (pos < 20) {
					byteHashes = byteHashes4;
				} else {
					byteHashes = byteHashes5;
				}
			} else {
				if (pos < 28) {
					byteHashes = byteHashes6;
				} else {
					byteHashes = byteHashes7;
				}
			}
		}
			
		return getSquashedHash(byteHashes, pos % HASHES_PER_SEGMENT);
	}
	
	public static final int getSquashedHash(final int byteHashes, int pos) {
		return (byteHashes >>> (24 - pos * 8)) & 0xFF;
	}

	public static final int shiftSquashedHash(final int squashedHash, int pos) {
		assert squashedHash == (squashedHash & 0xFF);
		return (squashedHash & 0xFF) << (24 - pos * 8);
	}
	
	public static final int combineTwoSquashedHashes(int h0, int h1) {
		return combineFourSquashedHashes(h0, h1, 0, 0);
	}
	
	public static final int combineFourSquashedHashes(int h0, int h1, int h2, int h3) {
		final int r0 = h0 << 24;
		final int r1 = h1 << 16;
		final int r2 = h2 <<  8;
		final int r3 = h3;
		return r0 ^ r1 ^ r2 ^ r3;
	}

	static final int HASHES_PER_SEGMENT = 4;

	static final int FIRST_HASH_INDEX = 0;
	static final int LAST_HASH_INDEX = HASHES_PER_SEGMENT - 1;
	
	static final int NUMBER_OF_SEGMENTS = 8;
	
	static final int FIRST_SEGMENT_INDEX = 0;
	static final int LAST_SEGMENT_INDEX = NUMBER_OF_SEGMENTS - 1;
	
	public static int[] arraycopyAndInsertInt(int input0, int input1, int input2, int input3, int input4,
			int input5, int input6, int input7, int idx, final int squashedHash) {
		// List(0, 1, 2, 3).foreach(idx => println(Integer.toHexString(0xFFFFFFFF << (8 * (3 - idx)))))
		// final int[] rangeMaskLR = new int[] { 0xFF000000, 0xFFFF0000, 0xFFFFFF00, 0xFFFFFFFF };
		
		// List(0, 1, 2, 3).foreach(idx => println(Integer.toHexString(0xFFFFFFFF >>> (8 * idx))))
		// final int[] rangeMaskRL = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x0000FFFF, 0x000000FF };

		final int[] inputs = new int[] { input0, input1, input2, input3, input4, input5, input6, input7 };
		final int[] outputs = new int[NUMBER_OF_SEGMENTS];
		
		// COPY SEGMENTS BEFORE	
		int segment = 0;
		for (; segment < NUMBER_OF_SEGMENTS && idx >= HASHES_PER_SEGMENT; segment++) {
			outputs[segment] = inputs[segment];
			idx -= HASHES_PER_SEGMENT;
		}

		// INSERT INTO SEGMENT	
		final int left;
		if (idx == FIRST_HASH_INDEX) {
			left = 0;
		} else {
			// left = inputs[segment] & rangeMaskLR[idx - 1]; 
			left = inputs[segment] & (0xFFFFFFFF << (8 * (LAST_HASH_INDEX - (idx - 1))));
		}
			
		final int middle = shiftSquashedHash(squashedHash, idx);
		
		final int right;
		if (idx == LAST_HASH_INDEX) {
			right = 0;
		} else {
			// right = (inputs[segment] >>> 8) & rangeMaskRL[idx + 1];
			right = (inputs[segment] >>> 8) & (0xFFFFFFFF >>> (8 * (idx + 1)));			
		}

		outputs[segment] = left | middle | right;
		int remainder = inputs[segment] & 0xFF;
		segment++;
		
		// SHIFT AND COPY AFTER
		for (; segment < NUMBER_OF_SEGMENTS; segment++) {
			outputs[segment] = (remainder << 24) ^ (inputs[segment] >>> 8);
			remainder = inputs[segment] & 0xFF;
		}
			
		return outputs;
	}
	
	public static int[] arraycopyAndRemoveInt(int input0, int input1, int input2, int input3, int input4,
			int input5, int input6, int input7, int idx) {		
		final int[] inputs = new int[] { input0, input1, input2, input3, input4, input5, input6, input7 };
		final int[] outputs = new int[NUMBER_OF_SEGMENTS];
		
		int segment = 0;
		
		// COPY SEGMENTS BEFORE	
		for (; segment < NUMBER_OF_SEGMENTS && idx >= HASHES_PER_SEGMENT; segment++) {
			outputs[segment] = inputs[segment];
			idx -= HASHES_PER_SEGMENT;
		}

		// REMOVE FROM SEGMENT	
		final int left;
		if (idx == FIRST_HASH_INDEX) {
			left = 0;
		} else {
			left = inputs[segment] & (0xFFFFFFFF << (8 * (LAST_HASH_INDEX - (idx - 1))));
		}
		
		final int middle;
		if (idx == LAST_HASH_INDEX) {
			middle = 0;
		} else {
			middle = (inputs[segment] << 8) & (0xFFFFFFFF >>> (8 * idx));
		}
		
		final int right;
		if (segment >= LAST_SEGMENT_INDEX) {
			right = 0;
		} else {
			right = inputs[segment + 1] >>> 24;
		}
		
		outputs[segment] = left | middle | right;
		segment++;
		
		// SHIFT AND COPY AFTER
		for (; segment < NUMBER_OF_SEGMENTS; segment++) {
			final int remainder;
			if (segment >= LAST_SEGMENT_INDEX) {
				remainder = 0;
			} else {
				remainder = inputs[segment + 1] >>> 24;
			}		
			
			outputs[segment] = (inputs[segment] << 8) ^ remainder;
		}
			
		return outputs;
	}
		
}
