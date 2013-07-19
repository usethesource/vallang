/*******************************************************************************
 * Copyright (c) 2013 CWI
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
	static Object[] arraycopyAndSet(Object[] array, int index, Object elementNew) {
		final Object[] arrayNew = new Object[array.length];
		System.arraycopy(array, 0, arrayNew, 0, array.length);
		arrayNew[index] = elementNew;
		return arrayNew;
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
