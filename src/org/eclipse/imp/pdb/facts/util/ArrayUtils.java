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
	static Object[] arraycopy(Object[] array) {
		final Object[] arrayNew = new Object[array.length];
		System.arraycopy(array, 0, arrayNew, 0, array.length);
		return arrayNew;
	}
	
	static Object[] arraycopyAndSet(Object[] array, int index, Object elementNew) {
		final Object[] arrayNew = new Object[array.length];
		System.arraycopy(array, 0, arrayNew, 0, array.length);
		arrayNew[index] = elementNew;
		return arrayNew;
	}

	static Object[] arraycopyAndMoveToBack(Object[] array, int indexOld, int indexNew, Object elementNew) {
		assert indexOld <= indexNew;		
		if (indexNew == indexOld) {
			return arraycopyAndSet(array, indexNew, elementNew);
		} else {
			final Object[] arrayNew = new Object[array.length];
			System.arraycopy(array, 0, arrayNew, 0, indexOld);
			System.arraycopy(array, indexOld + 1, arrayNew, indexOld, indexNew - indexOld);
			arrayNew[indexNew] = elementNew;
			System.arraycopy(array, indexNew + 1, arrayNew, indexNew + 1, array.length - indexNew - 1);
			return arrayNew;
		}
	}	

	static Object[] arraycopyAndMoveToFront(Object[] array, int indexOld, int indexNew, Object elementNew) {
		assert indexOld >= indexNew;
		if (indexNew == indexOld ) {
			return arraycopyAndSet(array, indexOld, elementNew);
		} else {
			final Object[] arrayNew = new Object[array.length];
			System.arraycopy(array, 0, arrayNew, 0, indexNew);
			arrayNew[indexNew] = elementNew;
			System.arraycopy(array, indexNew, arrayNew, indexNew + 1, indexOld - indexNew);
			System.arraycopy(array, indexOld + 1, arrayNew, indexOld + 1, array.length - indexOld - 1);
			return arrayNew;
		}
	}	
	
	static Object[] arraycopyAndInsert(Object[] array, int index, Object elementNew) {
		final Object[] arrayNew = new Object[array.length + 1];
		System.arraycopy(array, 0, arrayNew, 0, index);
		arrayNew[index] = elementNew;
		System.arraycopy(array, index, arrayNew, index + 1, array.length - index);
		return arrayNew;
	}

	static Object[] arraycopyAndRemove(Object[] array, int index) {
		final Object[] arrayNew = new Object[array.length - 1];
		System.arraycopy(array, 0, arrayNew, 0, index);
		System.arraycopy(array, index + 1, arrayNew, index, array.length - index - 1);
		return arrayNew;
	}
}
