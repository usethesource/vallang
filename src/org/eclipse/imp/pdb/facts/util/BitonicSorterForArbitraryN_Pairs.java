package org.eclipse.imp.pdb.facts.util;

/*
 * Source: http://www.iti.fh-flensburg.de/lang/algorithmen/sortieren/bitonic/oddn.htm
 * License unknown.
 */
public class BitonicSorterForArbitraryN_Pairs {
	protected static int[] a;
	protected static Object[] b;
	protected static int bOffset = 0;

	protected final static int LENGTH = 2;

	protected final static boolean ASCENDING = true;

	public static void sort(int[] a, Object[] b, int bOffset) {
		BitonicSorterForArbitraryN_Pairs.a = a;
		BitonicSorterForArbitraryN_Pairs.b = b;
		BitonicSorterForArbitraryN_Pairs.bOffset = bOffset;

		bitonicSort(0, a.length, ASCENDING);
	}

	private static void bitonicSort(int lo, int n, boolean dir) {
		if (n > 1) {
			int m = n / 2;
			bitonicSort(lo, m, !dir);
			bitonicSort(lo + m, n - m, dir);
			bitonicMerge(lo, n, dir);
		}
	}

	private static void bitonicMerge(int lo, int n, boolean dir) {
		if (n > 1) {
			int m = greatestPowerOfTwoLessThan(n);
			for (int i = lo; i < lo + n - m; i++)
				compare(i, i + m, dir);
			bitonicMerge(lo, m, dir);
			bitonicMerge(lo + m, n - m, dir);
		}
	}

	private static void compare(int i, int j, boolean dir) {
		if (dir == (a[i] > a[j]))
			exchange(i, j);
	}

	protected static void exchange(int i, int j) {
		// swap key
		int t = a[i];
		a[i] = a[j];
		a[j] = t;

		// swap first
		int k1 = bOffset + LENGTH * i;
		int l1 = bOffset + LENGTH * j;

		Object t1 = b[k1];
		b[k1] = b[l1];
		b[l1] = t1;

		// swap second
		k1++;
		l1++;

		t1 = b[k1];
		b[k1] = b[l1];
		b[l1] = t1;

		// // swap third
		// k1++;
		// l1++;
		//
		// t1 = b[k1];
		// b[k1] = b[l1];
		// b[l1] = t1;
	}

	private static int greatestPowerOfTwoLessThan(int n) {
		int k = 1;
		while (k < n)
			k = k << 1;
		return k >> 1;
	}
}