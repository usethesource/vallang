package org.eclipse.imp.pdb.facts.util;

public class SquashedHashCodeUtils {

	public static final int squashHash(final int h) {
		return (h ^ h >>> 20) & 0xFF;
	}
	
	public static final int getSquashedHash(final int byteHashes0, final int byteHashes1,
			final int byteHashes2, final int byteHashes3, final int byteHashes4,
			final int byteHashes5, final int byteHashes6, final int byteHashes7, int pos) {
		return getSquashedHashWithBinarySearch(byteHashes0, byteHashes1, byteHashes2, byteHashes3,
				byteHashes4, byteHashes5, byteHashes6, byteHashes7, pos);
	}	
	
	public static final int getSquashedHashWithSwitch(final int byteHashes0, final int byteHashes1,
			final int byteHashes2, final int byteHashes3, final int byteHashes4,
			final int byteHashes5, final int byteHashes6, final int byteHashes7, int pos) {
		final int byteHashes;
		
		final int segment = pos / ISEG.HASHES_PER_SEGMENT;
		final int indexInsideSegment = pos % ISEG.HASHES_PER_SEGMENT;
		
		switch (segment) {
		case 0:
			byteHashes = byteHashes0;
			break;
		case 1:
			byteHashes = byteHashes1;
			break;
		case 2:
			byteHashes = byteHashes2;
			break;
		case 3:
			byteHashes = byteHashes3;
			break;
		case 4:
			byteHashes = byteHashes4;
			break;
		case 5:
			byteHashes = byteHashes5;
			break;
		case 6:
			byteHashes = byteHashes6;
			break;
		case 7:			
			byteHashes = byteHashes7;
			break;
		default:
			// throw new IndexOutOfBoundsException(Integer.toString(pos));
			return -1;
		}
		
		return getSquashedHash(byteHashes, indexInsideSegment);
	}
	
	public static final int getSquashedHashWithBinarySearch(final int byteHashes0, final int byteHashes1,
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
			
		return getSquashedHash(byteHashes, pos % ISEG.HASHES_PER_SEGMENT);
	}
	
	public static final int getSquashedHash(final long byteHashes0, final long byteHashes1,
			final long byteHashes2, final long byteHashes3, int pos) {
		return getSquashedHashWithBinarySearch(byteHashes0, byteHashes1, byteHashes2, byteHashes3, pos);
	}	
	
	public static final int getSquashedHashWithSwitch(final long byteHashes0, final long byteHashes1,
			final long byteHashes2, final long byteHashes3, int pos) {
		final long byteHashes;
		
		final int segment = pos / LSEG.HASHES_PER_SEGMENT;
		final int indexInsideSegment = pos % LSEG.HASHES_PER_SEGMENT;
		
		switch (segment) {
		case 0:
			byteHashes = byteHashes0;
			break;
		case 1:
			byteHashes = byteHashes1;
			break;
		case 2:
			byteHashes = byteHashes2;
			break;
		case 3:
			byteHashes = byteHashes3;
			break;
		default:
			// throw new IndexOutOfBoundsException(Integer.toString(pos));
			return -1;
		}
		
		return getSquashedHash(byteHashes, indexInsideSegment);
	}
	
	public static final int getSquashedHashWithBinarySearch(final long byteHashes0, final long byteHashes1,
			final long byteHashes2, final long byteHashes3, int pos) {
		final long byteHashes;

		if (pos < 16) {
			if (pos < 8) {
				byteHashes = byteHashes0;
			} else {
				byteHashes = byteHashes1;
			}
		} else {
			if (pos < 24) {
				byteHashes = byteHashes2;
			} else {
				byteHashes = byteHashes3;
			}
		}
			
		return getSquashedHash(byteHashes, pos % LSEG.HASHES_PER_SEGMENT);
	}
		
	public static final int getSquashedHash(final int byteHashes, int pos) {
		return (byteHashes >>> (24 - pos * 8)) & 0xFF;
	}
	
	public static final int getSquashedHash(final long byteHashes, int pos) {
		return (int) ((byteHashes >>> (56 - pos * 8)) & 0xFF);
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
	
	static final class ISEG {
		static final int HASHES_PER_SEGMENT = 4;
		static final int NUMBER_OF_SEGMENTS = 8;
		
		static final int FIRST_HASH_INDEX = 0;
		static final int LAST_HASH_INDEX = HASHES_PER_SEGMENT - 1;
		
		static final int FIRST_SEGMENT_INDEX = 0;
		static final int LAST_SEGMENT_INDEX = NUMBER_OF_SEGMENTS - 1;	
	}

	static final class LSEG {
		static final int HASHES_PER_SEGMENT = 8;
		static final int NUMBER_OF_SEGMENTS = 4;
		
		static final int FIRST_HASH_INDEX = 0;
		static final int LAST_HASH_INDEX = HASHES_PER_SEGMENT - 1;
		
		static final int FIRST_SEGMENT_INDEX = 0;
		static final int LAST_SEGMENT_INDEX = NUMBER_OF_SEGMENTS - 1;	
	}	
	
	/*
	 * List(0, 1, 2, 3).foreach(idx => println(Integer.toHexString(0xFFFFFFFF << (8 * (3 - idx)))))
	 * final int[] rangeMaskLR = new int[] { 0xFF000000, 0xFFFF0000, 0xFFFFFF00, 0xFFFFFFFF };
	 * 
	 * List(0, 1, 2, 3).foreach(idx => println(Integer.toHexString(0xFFFFFFFF >>> (8 * idx))))
	 * final int[] rangeMaskRL = new int[] { 0xFFFFFFFF, 0x00FFFFFF, 0x0000FFFF, 0x000000FF };
	 */
	public static int[] arraycopyAndInsertIntAlt(int input0, int input1, int input2, int input3, int input4,
			int input5, int input6, int input7, int idx, final int squashedHash) {		
		final int[] inputs = new int[] { input0, input1, input2, input3, input4, input5, input6, input7 };
		final int[] outputs = new int[ISEG.NUMBER_OF_SEGMENTS];
		
		// COPY SEGMENTS BEFORE	
		int segment = 0;
		for (; segment < ISEG.NUMBER_OF_SEGMENTS && idx >= ISEG.HASHES_PER_SEGMENT; segment++) {
			outputs[segment] = inputs[segment];
			idx -= ISEG.HASHES_PER_SEGMENT;
		}

		// INSERT INTO SEGMENT	
		final int left;
		if (idx == ISEG.FIRST_HASH_INDEX) {
			left = 0;
		} else { 
			left = inputs[segment] & (0xFFFFFFFF << (8 * (ISEG.LAST_HASH_INDEX - (idx - 1))));
		}
			
		final int middle = shiftSquashedHash(squashedHash, idx);
		
		final int right;
		if (idx == ISEG.LAST_HASH_INDEX) {
			right = 0;
		} else {
			right = (inputs[segment] >>> 8) & (0xFFFFFFFF >>> (8 * (idx + 1)));			
		}

		outputs[segment] = left | middle | right;
		int remainder = inputs[segment] & 0xFF;
		segment++;
		
		// SHIFT AND COPY AFTER
		for (; segment < ISEG.NUMBER_OF_SEGMENTS; segment++) {
			outputs[segment] = (remainder << 24) ^ (inputs[segment] >>> 8);
			remainder = inputs[segment] & 0xFF;
		}
			
		return outputs;
	}
	
	public static int[] arraycopyAndInsertInt(int input0, int input1, int input2,
			int input3, int input4, int input5, int input6, int input7, int idx,
			final int squashedHash) {
		// COPY SEGMENTS 
		int output0 = input0;
		int output1 = input1;
		int output2 = input2;
		int output3 = input3;
		int output4 = input4;
		int output5 = input5;
		int output6 = input6;
		int output7 = input7;
		
		final int segment = idx / ISEG.HASHES_PER_SEGMENT;
		final int indexInsideSegment = idx % ISEG.HASHES_PER_SEGMENT;

		// INSERT INTO SEGMENT					
		switch(segment) {
		case 0: output0 = insertAndShift(input0, indexInsideSegment, squashedHash); break;
		case 1: output1 = insertAndShift(input1, indexInsideSegment, squashedHash); break;
		case 2: output2 = insertAndShift(input2, indexInsideSegment, squashedHash); break;
		case 3: output3 = insertAndShift(input3, indexInsideSegment, squashedHash); break;
		case 4: output4 = insertAndShift(input4, indexInsideSegment, squashedHash); break;
		case 5: output5 = insertAndShift(input5, indexInsideSegment, squashedHash); break;
		case 6: output6 = insertAndShift(input6, indexInsideSegment, squashedHash); break;
		case 7: output7 = insertAndShift(input7, indexInsideSegment, squashedHash); break;
		} 
		
		// SHIFT AND COPY AFTER
		switch (segment + 1) {
		case 1:
			output1 = (input0 << 24) ^ (input1 >>> 8);
		case 2:
			output2 = (input1 << 24) ^ (input2 >>> 8);
		case 3:
			output3 = (input2 << 24) ^ (input3 >>> 8);
		case 4:
			output4 = (input3 << 24) ^ (input4 >>> 8);
		case 5:
			output5 = (input4 << 24) ^ (input5 >>> 8);
		case 6:
			output6 = (input5 << 24) ^ (input6 >>> 8);
		case 7:
			output7 = (input6 << 24) ^ (input7 >>> 8);			
		}
			
		return new int[] { output0, output1, output2, output3, output4, output5, output6, output7 };
	}

	public static int insertAndShift(final int hashes, final int idx, final int hash) {
		final int left;
		if (idx == ISEG.FIRST_HASH_INDEX) {
			left = 0;
		} else { 
			left = hashes & (0xFFFFFFFF << (8 * (ISEG.LAST_HASH_INDEX - (idx - 1))));
		}
					
		final int middle = (hash & 0xFF) << (24 - idx * 8);

		final int right;
		if (idx == ISEG.LAST_HASH_INDEX) {
			right = 0;
		} else {
			right = (hashes >>> 8) & (0xFFFFFFFF >>> (8 * (idx + 1)));			
		}

		final int output = left | middle | right;
		return output;
	}	
	
	public static int insertAndShiftV2(final int hashes, final int idx, final int hash) {
		final int offset = 8 * idx;
		final int offsetFromRight = 32 - offset;
		
		final int middle = (hash << 24) >>> offset;
		
		if (offset == 0) {
			final int right = (hashes << offset) >>> (offset + 8);
			return middle | right;
		} else {
			final int left = (hashes >>> offsetFromRight) << (offsetFromRight);

			if (offset == 24) {
				return left | middle;
			} else {
				final int right = (hashes << offset) >>> (offset + 8);
				return left | middle | right;
			}
		}
	}	
		
	public static int[] arraycopyAndRemoveInt(int input0, int input1, int input2, int input3, int input4,
			int input5, int input6, int input7, int idx) {				
		final int[] inputs = new int[] { input0, input1, input2, input3, input4, input5, input6, input7 };
		final int[] outputs = new int[ISEG.NUMBER_OF_SEGMENTS];
		
		int segment = 0;
		
		// COPY SEGMENTS BEFORE	
		for (; segment < ISEG.NUMBER_OF_SEGMENTS && idx >= ISEG.HASHES_PER_SEGMENT; segment++) {
			outputs[segment] = inputs[segment];
			idx -= ISEG.HASHES_PER_SEGMENT;
		}

		// REMOVE FROM SEGMENT	
		final int left;
		if (idx == ISEG.FIRST_HASH_INDEX) {
			left = 0;
		} else {
			left = inputs[segment] & (0xFFFFFFFF << (8 * (ISEG.LAST_HASH_INDEX - (idx - 1))));
		}
		
		final int middle;
		if (idx == ISEG.LAST_HASH_INDEX) {
			middle = 0;
		} else {
			middle = (inputs[segment] << 8) & (0xFFFFFFFF >>> (8 * idx));
		}
		
		final int right;
		if (segment >= ISEG.LAST_SEGMENT_INDEX) {
			right = 0;
		} else {
			right = inputs[segment + 1] >>> 24;
		}
		
		outputs[segment] = left | middle | right;
		segment++;
		
		// SHIFT AND COPY AFTER
		for (; segment < ISEG.NUMBER_OF_SEGMENTS; segment++) {
			final int remainder;
			if (segment >= ISEG.LAST_SEGMENT_INDEX) {
				remainder = 0;
			} else {
				remainder = inputs[segment + 1] >>> 24;
			}		
			
			outputs[segment] = (inputs[segment] << 8) ^ remainder;
		}
			
		return outputs;
	}
		
}
