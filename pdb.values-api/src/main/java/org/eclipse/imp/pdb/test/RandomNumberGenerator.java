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

import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.IValueFactory;

/**
 * Random INumber generator.
 * 
 * Generates numbers based on RandomIntegerGenerator, RandomRationalGenerator and RandomRealGenerator
 * @author anya
 *
 */
public class RandomNumberGenerator extends RandomGenerator<INumber> {

	private final RandomIntegerGenerator ints;
	private final RandomRealGenerator reals;
	private final RandomRationalGenerator rats;

	public RandomNumberGenerator(IValueFactory vf) {
		super(vf);
		this.ints = new RandomIntegerGenerator(vf);
		this.reals= new RandomRealGenerator(vf);
		this.rats = new RandomRationalGenerator(vf);
	}
	
	@Override
	public INumber next() {
		int i = random.nextInt(3);
		if(i == 0)
			return ints.next();
		else if(i == 1)
			return reals.next();
		else
			return rats.next();
	}

}
