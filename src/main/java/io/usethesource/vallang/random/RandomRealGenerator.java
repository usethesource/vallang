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

import io.usethesource.vallang.IReal;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.type.TypeFactory;

/**
 * Random IReal generator.
 * 
 * Generates reals in the range 0 ..  +/- Double.MAX_VALUE
 * @author anya
 *
 */
public class RandomRealGenerator extends RandomGenerator<IReal> {


    public RandomRealGenerator(IValueFactory vf) {
		super(vf);
	}
	
	@Override
	public IReal next() {
	    return (IReal) generator.visitReal(TypeFactory.getInstance().realType());
	}

}
