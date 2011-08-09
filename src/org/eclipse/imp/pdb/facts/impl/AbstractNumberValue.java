/*******************************************************************************
* Copyright (c) 2010 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   Jurgen Vinju (jurgen.vinju@cwi.nl) - initial API and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public abstract class AbstractNumberValue extends Value implements INumber {
	public AbstractNumberValue(Type type) {
		super(type);
	}

	public INumber add(INumber other) {
		if (other.getType().isRealType()) {
			return add(other.toReal());
		}
		if (other.getType().isIntegerType()) {
			return add(other.toInteger());
		}
		if (other.getType().isRationalType()) {
			return add(other.toRational());
		}
		
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}
	
	public INumber divide(INumber other, int precision) {
		if (other.getType().isRealType()) {
			return divide(other.toReal(), precision);
		}
		if (other.getType().isIntegerType()) {
			return divide(other.toInteger(), precision);
		}
		if (other.getType().isRationalType()) {
			return divide(other.toRational(), precision);
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}

	public IBool greater(INumber other) {
		if (other.getType().isRealType()) {
			return greater(other.toReal());
		}
		if (other.getType().isIntegerType()) {
			return greater(other.toInteger());
		}
		if (other.getType().isRationalType()) {
			return greater(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}
	
	public IBool greaterEqual(INumber other) {
		if (other.getType().isRealType()) {
			return greaterEqual(other.toReal());
		}
		if (other.getType().isIntegerType()) {
			return greaterEqual(other.toInteger());
		}
		if (other.getType().isRationalType()) {
			return greaterEqual(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}
	
	public IBool less(INumber other) {
		if (other.getType().isRealType()) {
			return less(other.toReal());
		}
		if (other.getType().isIntegerType()) {
			return less(other.toInteger());
		}
		if (other.getType().isRationalType()) {
			return less(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}
	
	public IBool lessEqual(INumber other) {
		if (other.getType().isRealType()) {
			return lessEqual(other.toReal());
		}
		if (other.getType().isIntegerType()) {
			return lessEqual(other.toInteger());
		}
		if (other.getType().isRationalType()) {
			return lessEqual(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}

	public INumber multiply(INumber other) {
		if (other.getType().isRealType()) {
			return multiply(other.toReal());
		}
		if (other.getType().isIntegerType()) {
			return multiply(other.toInteger());
		}
		if (other.getType().isRationalType()) {
			return multiply(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}

	public INumber subtract(INumber other) {
		if (other.getType().isRealType()) {
			return subtract(other.toReal());
		}
		if (other.getType().isIntegerType()) {
			return subtract(other.toInteger());
		}
		if (other.getType().isRationalType()) {
			return subtract(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}
}
