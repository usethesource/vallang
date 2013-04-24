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
package org.eclipse.imp.pdb.facts;

import java.util.Iterator;

public interface IContainer extends Iterable<IValue> {

	boolean isEmpty();

    boolean hasCount();
	
	int getCount();
		

	
	boolean contains(IValue query);
	
	IValue get(IValue query);
	

	
	int getArity();
	
	@Override
	Iterator<IValue> iterator();
	
	Iterator<IValue> iterator(int arity);
		
}
