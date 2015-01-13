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
package org.eclipse.imp.pdb.facts.util;

public class ArrayUtils {
	public static Object[] copy(Object[] array) {
		final Object[] arrayNew = new Object[array.length];
		System.arraycopy(array, 0, arrayNew, 0, array.length);
		return arrayNew;
	}
	
	public static Object[] copyAndSet(Object[] array, int index, Object elementNew) {
		final Object[] arrayNew = new Object[array.length];
		System.arraycopy(array, 0, arrayNew, 0, array.length);
		arrayNew[index] = elementNew;
		return arrayNew;
	}
	
	public static Object[] copyAndMoveToBack(Object[] array, int indexOld, int indexNew, Object elementNew) {
		assert indexOld <= indexNew;		
		if (indexNew == indexOld) {
			return copyAndSet(array, indexNew, elementNew);
		} else {
			final Object[] arrayNew = new Object[array.length];
			System.arraycopy(array, 0, arrayNew, 0, indexOld);
			System.arraycopy(array, indexOld + 1, arrayNew, indexOld, indexNew - indexOld);
			arrayNew[indexNew] = elementNew;
			System.arraycopy(array, indexNew + 1, arrayNew, indexNew + 1, array.length - indexNew - 1);
			return arrayNew;
		}
	}	

	/**
	 * Shrinks the array by 1 (key, val pair becomes a node) and moves...
	 */
	public static Object[] copyAndMoveToBackPair(Object[] array, int indexOld, int indexNew, Object nodeNew) {
		assert indexOld <= indexNew;		

		final Object[] arrayNew = new Object[array.length - 1];
		System.arraycopy(array, 0, arrayNew, 0, indexOld);
		System.arraycopy(array, indexOld + 2, arrayNew, indexOld, indexNew - indexOld);
		arrayNew[indexNew] = nodeNew;
		System.arraycopy(array, indexNew + 2, arrayNew, indexNew + 1, array.length - indexNew - 2);
		return arrayNew;
	}	
	
	public static Object[] copyAndMoveToFront(Object[] array, int indexOld, int indexNew, Object elementNew) {
		assert indexOld >= indexNew;
		if (indexNew == indexOld ) {
			return copyAndSet(array, indexOld, elementNew);
		} else {
			final Object[] arrayNew = new Object[array.length];
			System.arraycopy(array, 0, arrayNew, 0, indexNew);
			arrayNew[indexNew] = elementNew;
			System.arraycopy(array, indexNew, arrayNew, indexNew + 1, indexOld - indexNew);
			System.arraycopy(array, indexOld + 1, arrayNew, indexOld + 1, array.length - indexOld - 1);
			return arrayNew;
		}
	}	
	
	/**
	 * Enlarges the array by 1 (a node becomes a key, val pair) and moves...
	 */
	public static Object[] copyAndMoveToFrontPair(Object[] array, int indexOld, int indexNew, Object keyNew, Object valNew) {
		assert indexOld >= indexNew;

		final Object[] arrayNew = new Object[array.length+1];
		System.arraycopy(array, 0, arrayNew, 0, indexNew);
		arrayNew[indexNew] = keyNew;
		arrayNew[indexNew+1] = valNew;
		System.arraycopy(array, indexNew, arrayNew, indexNew + 2, indexOld - indexNew);
		System.arraycopy(array, indexOld + 1, arrayNew, indexOld + 2, array.length - indexOld - 1);
		return arrayNew;
	}	
		
	public static Object[] copyAndInsert(Object[] array, int index, Object elementNew) {
		final Object[] arrayNew = new Object[array.length + 1];
		System.arraycopy(array, 0, arrayNew, 0, index);
		arrayNew[index] = elementNew;
		System.arraycopy(array, index, arrayNew, index + 1, array.length - index);
		return arrayNew;
	}
	
	public static Object[] copyAndInsertPair(Object[] array, int index, Object keyNew, Object valNew) {
		final Object[] arrayNew = new Object[array.length + 2];
		System.arraycopy(array, 0, arrayNew, 0, index);
		arrayNew[index] = keyNew;
		arrayNew[index+1] = valNew;
		System.arraycopy(array, index, arrayNew, index + 2, array.length - index);
		return arrayNew;
	}	

	public static Object[] copyAndRemove(Object[] array, int index) {
		final Object[] arrayNew = new Object[array.length - 1];
		System.arraycopy(array, 0, arrayNew, 0, index);
		System.arraycopy(array, index + 1, arrayNew, index, array.length - index - 1);
		return arrayNew;
	}
	
	public static Object[] copyAndRemovePair(Object[] array, int index) {
		final Object[] arrayNew = new Object[array.length - 2];
		System.arraycopy(array, 0, arrayNew, 0, index);
		System.arraycopy(array, index + 2, arrayNew, index, array.length - index - 2);
		return arrayNew;
	}
	
}
