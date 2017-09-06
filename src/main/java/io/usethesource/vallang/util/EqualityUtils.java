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
package io.usethesource.vallang.util;

import io.usethesource.capsule.util.EqualityComparator;
import io.usethesource.vallang.IValue;

public class EqualityUtils {

	/**
	 * Temporary function in order to support different equality checks.
	 */
	@SuppressWarnings("rawtypes")
	public static EqualityComparator<Object> getDefaultEqualityComparator() {
		return Object::equals;
	}

	/**
	 * Temporary function in order to support equivalence. Note, this
	 * implementation is only works for {@link IValue} arguments. If arguments
	 * are of a different type, an unchecked exception will be thrown.
	 */
	@SuppressWarnings("rawtypes")
	public static EqualityComparator<Object> getEquivalenceComparator() {
		return (a, b) -> EqualityComparator.equals((IValue) a, (IValue) b, IValue::isEqual);
	}

}
