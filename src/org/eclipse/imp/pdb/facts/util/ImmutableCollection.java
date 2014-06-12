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

import java.util.Collection;
import java.util.Comparator;

public interface ImmutableCollection<E> extends Collection<E> {

    boolean contains(Object o);
    
	boolean containsEquivalent(Object o, Comparator<Object> cmp);

	boolean containsAll(Collection<?> c);
	
	boolean containsAllEquivalent(Collection<?> c, Comparator<Object> cmp);
	
}
