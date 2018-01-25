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
package io.usethesource.vallang.random.deprecated;

import java.util.Random;

import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.random.RandomValueGenerator;

/**
 * Abstract interface to random generators for IValues.
 * 
 * @author anya
 *
 */
public abstract class RandomGenerator<T> {
	protected final Random random;
	protected final IValueFactory vf;
    protected final RandomValueGenerator generator;

	public RandomGenerator(IValueFactory vf) {
		this.vf = vf;
		this.random = new Random();
	    this.generator = new RandomValueGenerator(vf, random, 5, 5, true);
	}
	
	/**
	 * @return the next random value for the generator
	 */
	public abstract T next();
}
