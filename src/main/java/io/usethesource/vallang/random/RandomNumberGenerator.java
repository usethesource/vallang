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
package io.usethesource.vallang.random;

import io.usethesource.vallang.INumber;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory;

/**
 * Random INumber generator.
 * 
 * Generates numbers based on RandomIntegerGenerator, RandomRationalGenerator and RandomRealGenerator
 * @author anya
 *
 */
public class RandomNumberGenerator extends RandomGenerator<INumber> {

	public RandomNumberGenerator(IValueFactory vf) {
		super(vf);
	}
	
	@Override
	public INumber next() {
	    return (INumber) generator.visitNumber(TypeFactory.getInstance().numberType());
	}

}
