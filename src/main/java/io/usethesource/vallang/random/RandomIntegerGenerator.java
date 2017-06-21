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

import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory;

/**
 * Random IInteger generator.
 * 
 * Generates integers in the range 0 ..  +/- Long.MAX_VALUE ^ 2 + Integer.MAX_VALUE
 * @author anya
 *
 */
public class RandomIntegerGenerator extends RandomGenerator<IInteger> {

	public RandomIntegerGenerator(IValueFactory vf) {
		super(vf);
	}
	
	@Override
	public IInteger next() {
	    return (IInteger) generator.visitInteger(TypeFactory.getInstance().integerType());
	}

}
