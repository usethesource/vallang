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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.util.EqualityComparator;
import io.usethesource.vallang.IValue;

public class EqualityUtils {

	/**
	 * Temporary function in order to support different equality checks.
	 */
	public static EqualityComparator<Object> getDefaultEqualityComparator() {
		return Object::equals;
	}

	/**
	 * Temporary function in order to support equivalence. Note, this
	 * implementation is only works for {@link IValue} arguments. If arguments
	 * are of a different type, an unchecked exception will be thrown.
	 */
	public static EqualityComparator<Object> getEquivalenceComparator() {
		return (a, b) -> EqualityComparator.equals((IValue) a, (IValue) b, IValue::isEqual);
	}

	public static final EqualityComparator<Map.Immutable<String, IValue>> KEYWORD_PARAMETER_COMPARATOR = EqualityUtils::compareKwParams;
			
    private static boolean compareKwParams(Map.Immutable<String, IValue> a, Map.Immutable<String, IValue> b) {
        if (a.size() != b.size()) {
            return false;
        }
        
        Iterator<Entry<String, IValue>> aIt = a.entryIterator();
        Iterator<Entry<String, IValue>> bIt = a.entryIterator();
        EqualityComparator<Object> comp = getEquivalenceComparator();
        
        while (aIt.hasNext() && bIt.hasNext()) {
            Entry<String, IValue> aNext = aIt.next();
            Entry<String, IValue> bNext = bIt.next();
            
            if (!aNext.getKey().equals(bNext.getKey())) {
                return false;
            }
            
            if (!comp.equals(aNext.getValue(), bNext.getValue())) {
                return false;
            }
        }
        
        return true;
    }

    public static final BiFunction<java.util.List<Object>, Object, Boolean> func = (a, b) -> a.stream().map(e -> e == b) == null;
}
