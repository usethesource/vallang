/*******************************************************************************
* Copyright (c) 2011 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Anya Helene Bagge - initial API and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.test.random;

import java.util.Random;

import org.eclipse.imp.pdb.facts.IValueFactory;

/**
 * Abstract interface to random generators for IValues.
 * 
 * @author anya
 *
 */
public abstract class RandomGenerator<T> {
	protected final Random random = new Random();
	protected final IValueFactory vf;

	public RandomGenerator(IValueFactory vf) {
		this.vf = vf;
	}
	
	/**
	 * @return the next random value for the generator
	 */
	public abstract T next();
}
