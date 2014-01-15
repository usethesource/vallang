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

import java.util.Comparator;
import java.util.Set;

public interface TransientSet<E> { // extends ImmutableCollection<E>, Set<E> {

	boolean __insert(E e);
	
	boolean __insertEquivalent(E e, Comparator<Object> cmp);

	boolean __insertAll(Set<? extends E> set);	
	
	boolean __insertAllEquivalent(Set<? extends E> set, Comparator<Object> cmp);
	
	boolean __remove(E e);
	
	boolean __removeEquivalent(E e, Comparator<Object> cmp);

	boolean __removeAll(Set<? extends E> set);
	
	boolean __removeAllEquivalent(Set<? extends E> set, Comparator<Object> cmp);
	
	ImmutableSet<E> freeze();
	
}
