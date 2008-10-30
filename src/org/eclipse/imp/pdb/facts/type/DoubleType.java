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

public class DoubleType extends Type {
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
	
	@Override
	public boolean isSubtypeOf(Type other) {
		if (other.isValueType()) {
			return true;
		} else {
			return other == this;
		}
	}

	@Override
	public Type lub(Type other) {
		if (other.isSubtypeOf(this)) {
			return this;
		}
		else {
			return TypeFactory.getInstance().valueType();
		}
	}

	@Override
	public String getTypeDescriptor() {
		return toString();
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
}
