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
package org.eclipse.imp.pdb.facts.impl.shared;

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.impl.RationalValue;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;

public class SharedRationalValue extends RationalValue implements IShareable {
	private static final IInteger SHARED_ONE = SharedValueFactory.getInstance().integer(1);

	public SharedRationalValue(IInteger num, IInteger denom) {
		super(num, denom);
	}
	
	public boolean equivalent(IShareable shareable) {
		return super.equals(shareable);
	}

	public boolean equals(Object o){
		return (this == o);
	}
	
	@Override
	protected IInteger intOne() {
		return SHARED_ONE;
	}
	
	@Override
	public IRational toRational(IInteger a, IInteger b) {
		return new RationalValue(a, b);
	}
}
