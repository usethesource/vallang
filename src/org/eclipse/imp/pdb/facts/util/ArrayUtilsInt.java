package org.eclipse.imp.pdb.facts.util;

public class ArrayUtilsInt {

	static int[] arraycopyAndInsertInt(final int[] src, final int idx, final int value) {
		final int[] dst = new int[src.length + 1];
	
		// copy 'src' and insert 1 element(s) at position 'idx'
		System.arraycopy(src, 0, dst, 0, idx);
		dst[idx] = value;
		System.arraycopy(src, idx, dst, idx + 1, src.length - idx);
	
		return dst;
	}

	static int[] arraycopyAndRemoveInt(final int[] src, final int idx) {
		final int[] dst = new int[src.length - 1];
	
		// copy 'src' and remove 1 element(s) at position 'idx'
		System.arraycopy(src, 0, dst, 0, idx);
		System.arraycopy(src, idx + 1, dst, idx, src.length - idx - 1);
		
		return dst;
	}

}
