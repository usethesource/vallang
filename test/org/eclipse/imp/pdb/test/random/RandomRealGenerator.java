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

import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IValueFactory;

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
		IReal r;
		if(random.nextInt(2) == 0)
			r = vf.real(random.nextDouble());
		else
			r = vf.real(-random.nextDouble());
		// TODO: make large numbers by tweaking the exponent
		// int exp = (int) (random.nextInt(308)*random.nextGaussian());
	
		return r;
	}

}
