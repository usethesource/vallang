/*******************************************************************************
 * Copyright (c) 2014-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Davy Landman - Davy.Landman@cwi.nl - CWI  
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.util;

// based on https://github.com/Cyan4973/xxHash
// we use the endian-dependance version since we do not 
// use the hashes across machines.

public class XXHash32 {
	private static final int PRIME1 = (int) 2654435761L;
	private static final int PRIME2 = (int) 2246822519L;
	private static final int PRIME3 = (int) 3266489917L;
	private static final int PRIME4 = 668265263;
	private static final int PRIME5 = 0x165667b1;

	public static int hashInt(int n) {
		return hashInt(0, n);
	}

	public static int hashInt(int seed, int n) {
		int h32 = seed + PRIME5;
		
		h32 += n * PRIME3;
		h32 = Integer.rotateLeft(h32, 17) * PRIME4;
		
		h32 ^= h32 >>> 15;
		h32 *= PRIME2;
		h32 ^= h32 >>> 13;
		h32 *= PRIME3;
		h32 ^= h32 >>> 16;

		return h32;
	}
	public static int hashTwoInts(int a, int b) {
		return hashTwoInts(0, a, b);
	}
	public static int hashTwoInts(int seed, int a, int b) {
		int h32 = seed + PRIME5;
		
		h32 += a * PRIME3;
		h32 = Integer.rotateLeft(h32, 17) * PRIME4;
		h32 += b * PRIME3;
		h32 = Integer.rotateLeft(h32, 17) * PRIME4;
		
		h32 ^= h32 >>> 15;
		h32 *= PRIME2;
		h32 ^= h32 >>> 13;
		h32 *= PRIME3;
		h32 ^= h32 >>> 16;

		return h32;
	}
	public static int hashThreeInts(int a, int b, int c) {
		return hashThreeInts(0, a, b, c);
	}
	public static int hashThreeInts(int seed, int a, int b, int c) {
		int h32 = seed + PRIME5;
		
		h32 += a * PRIME3;
		h32 = Integer.rotateLeft(h32, 17) * PRIME4;
		h32 += b * PRIME3;
		h32 = Integer.rotateLeft(h32, 17) * PRIME4;
		h32 += c * PRIME3;
		h32 = Integer.rotateLeft(h32, 17) * PRIME4;
		
		h32 ^= h32 >>> 15;
		h32 *= PRIME2;
		h32 ^= h32 >>> 13;
		h32 *= PRIME3;
		h32 ^= h32 >>> 16;

		return h32;
	}
	
	public static int hashLong(long n) {
		return hashLong(0, n);
	}
	public static int hashLong(int seed, long n) {
		return hashTwoInts(seed, (int)n, (int) (n >>> 32));
	}
	/**
	 * A version with an unrolled loop for speed.
	 * Unrolling happens when data.length >= 4
	 * @param seed seed to use
	 * @param data data array
	 * @return hash calculated by the xxHash algorithm.
	 */
	public static int hashInts(int seed, int... data) {
		int h32;
		int i = 0;
		int len = data.length;
		
		if (len >= 4) {
			int limit = len - 4;
			int v1 = seed + PRIME1 + PRIME2;
			int v2 = seed + PRIME2;
			int v3 = seed + 0;
			int v4 = seed - PRIME1;
			do {
				v1 += data[i] * PRIME2;
				v1 = Integer.rotateLeft(v1, 13);
				v1 *= PRIME1;
				i++;
				v2 += data[i] * PRIME2;
				v2 = Integer.rotateLeft(v2, 13);
				v2 *= PRIME1;
				i++;
				v3 += data[i++] * PRIME2;
				v3 = Integer.rotateLeft(v3, 13);
				v3 *= PRIME1;
				i++;
				v4 += data[i++] * PRIME2;
				v4 = Integer.rotateLeft(v4, 13);
				v4 *= PRIME1;
				i++;
			}
			while (i <= limit);
			h32 = Integer.rotateLeft(v1, 1) + Integer.rotateLeft(v2, 7) + Integer.rotateLeft(v3, 12) + Integer.rotateLeft(v4, 18);
		}
		else {
			h32 = seed + PRIME5;
		}
		h32 += len;
		while (i < len) {
			h32 += data[i] * PRIME3;
			h32 = Integer.rotateLeft(h32, 17) * PRIME4;
			i++;
		}
		h32 ^= h32 >>> 15;
		h32 *= PRIME2;
		h32 ^= h32 >>> 13;
		h32 *= PRIME3;
		h32 ^= h32 >>> 16;

		return h32;
	}
}
