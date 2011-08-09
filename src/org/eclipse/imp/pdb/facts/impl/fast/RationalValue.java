/*******************************************************************************
* Copyright (c) 2011 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Anya Helene Bagge - initial implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IRational;

public class RationalValue extends org.eclipse.imp.pdb.facts.impl.RationalValue {
	private static final IInteger FAST_ONE = ValueFactory.getInstance().integer(1);

	public RationalValue(IInteger num, IInteger denom) {
		super(num, denom);
	}

	@Override
	protected IInteger intOne() {
		return FAST_ONE;
	}
	
	@Override
	public IRational toRational(IInteger a, IInteger b) {
		return new RationalValue(a, b);
	}
}
