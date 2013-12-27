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

import java.util.Set;

public interface ImmutableJdkSet<E> extends Set<E> {
	
	ImmutableJdkSet<E> __insert(E e);

	ImmutableJdkSet<E> __insertAll(Set<? extends E> set);	
	
	ImmutableJdkSet<E> __remove(E e);

}