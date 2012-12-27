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

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IValueFactory;

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
		IInteger i = vf.integer(random.nextLong());
		// make a few really huge numbers as well.
		while(random.nextInt(5) == 1) {
			i = i.multiply(vf.integer(random.nextLong())).add(vf.integer(random.nextInt()));
		}
		return i;
	}

}
