/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
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

import java.util.Set;

public interface TransientSet<E> { // extends ImmutableCollection<E>, Set<E> {

	boolean __insert(E e);
	
	boolean __insertAll(Set<? extends E> set);
	
	boolean __remove(E e);
	
	ImmutableSet<E> freeze();
	
}
