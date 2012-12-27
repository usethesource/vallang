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
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IValueFactory;

/**
 * Random IRational generator.
 * 
 * Generates numerators/denominators based on the RandomIntegerGenerator.
 * @author anya
 *
 */
public class RandomRationalGenerator extends RandomGenerator<IRational> {

	private final RandomIntegerGenerator intGen;

	public RandomRationalGenerator(IValueFactory vf) {
		super(vf);
		intGen = new RandomIntegerGenerator(vf);
	}
	
	@Override
	public IRational next() {
		IInteger a = intGen.next();
		IInteger b = intGen.next();
		if(b.isEqual(vf.integer(0)))
			b = vf.integer(1);
		return vf.rational(a, b);
	}

}
