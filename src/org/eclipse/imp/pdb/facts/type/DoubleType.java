/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

/*package*/ final class DoubleType extends Type {
	private final static DoubleType sInstance = new DoubleType();

	/* package */static DoubleType getInstance() {
		return sInstance;
	}

	private DoubleType() {
	}

	@Override
	public boolean isDoubleType() {
		return true;
	}
	
	/**
	 * Should never need to be called; there should be only one instance of
	 * IntegerType
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof DoubleType);
	}

	@Override
	public int hashCode() {
		return 84121;
	}

	@Override
	public String toString() {
		return "double";
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitDouble(this);
	}
	
	@Override
	public IValue make(IValueFactory f, double arg) {
		return f.dubble(arg);
	}
	
	@Override
	public IValue make(IValueFactory f, TypeStore store, double arg) {
		return make(f, arg);
	}
}
