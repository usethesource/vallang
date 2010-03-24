/*******************************************************************************
* Copyright (c) 2010 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju (Jurgen.Vinju@cwi.nl) - initial API and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class BoolValue extends Value implements IBool {
	private final boolean fValue;

	/*package*/ BoolValue(boolean b) {
		super(TypeFactory.getInstance().boolType());
		this.fValue = b;
	}
	
	public boolean getValue() {
		return fValue;
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitBoolean(this);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (getClass() == obj.getClass()) {
			return fValue == ((BoolValue) obj).fValue;
		}
		return false;
	}
	
	public IBool and(IBool other) {
		return new BoolValue(fValue && other.getValue());
	}
	
	public IBool or(IBool other) {
		return new BoolValue(fValue || other.getValue());
	}
	
	public IBool not() {
		return new BoolValue(!fValue);
	}
	
	public IBool implies(IBool other) {
		return new BoolValue(fValue ? other.getValue() : true);
	}
	
	public IBool equivalent(IBool other) {
		return new BoolValue(fValue == other.getValue());
	}
	
	public IBool xor(IBool other) {
		return new BoolValue(fValue ^ other.getValue());
	}
	
	@Override
	public int hashCode() {
		return fValue ? 1231 : 1237;
	}
	
	public String getStringRepresentation() {
		return fValue ? "true" : "false";
	}
}
