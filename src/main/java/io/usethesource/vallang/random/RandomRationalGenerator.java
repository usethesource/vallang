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

import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory;

/**
 * Random IRational generator.
 * 
 * Generates numerators/denominators based on the RandomIntegerGenerator.
 * @author anya
 *
 */
public class RandomRationalGenerator extends RandomGenerator<IRational> {


	public RandomRationalGenerator(IValueFactory vf) {
		super(vf);
	}
	
	@Override
	public IRational next() {
	    return (IRational) generator.visitRational(TypeFactory.getInstance().rationalType());
	}

}
