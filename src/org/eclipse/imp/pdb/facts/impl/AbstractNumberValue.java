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
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public abstract class AbstractNumberValue extends Value implements INumber {
	public AbstractNumberValue(Type type) {
		super(type);
	}

	public INumber add(INumber other) {
		if (isRealType(other)) {
			return add(other.toReal());
		}
		if (isIntegerType(other)) {
			return add(other.toInteger());
		}
		if (isRationalType(other)) {
			return add(other.toRational());
		}
		
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}
	
	public boolean isEqual(IValue other) {
	  if (other instanceof INumber) {
	    return equal((INumber) other).getValue();
	  }
	  return super.isEqual(other);
	}
	
	public INumber divide(INumber other, int precision) {
		if (isRealType(other)) {
			return divide(other.toReal(), precision);
		}
		if (isIntegerType(other)) {
			return divide(other.toInteger(), precision);
		}
		if (isRationalType(other)) {
			return divide(other.toRational(), precision);
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}

	public IBool greater(INumber other) {
		if (isRealType(other)) {
			return greater(other.toReal());
		}
		if (isIntegerType(other)) {
			return greater(other.toInteger());
		}
		if (isRationalType(other)) {
			return greater(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}
	
	public IBool greaterEqual(INumber other) {
	  if (isRealType(other)) {
			return greaterEqual(other.toReal());
		}
	  if (isIntegerType(other)) {
			return greaterEqual(other.toInteger());
		}
		if (isRationalType(other)) {
			return greaterEqual(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}

	protected boolean isRationalType(INumber other) {
    return other.getType().equivalent(TypeFactory.getInstance().rationalType());
  }

  protected boolean isIntegerType(INumber other) {
    return other.getType().equivalent(TypeFactory.getInstance().integerType());
  }

  protected boolean isRealType(INumber other) {
    return other.getType().equivalent(TypeFactory.getInstance().realType());
  }
	
	public IBool less(INumber other) {
	  if (isRealType(other)) {
			return less(other.toReal());
		}
	  if (isIntegerType(other)) {
			return less(other.toInteger());
		}
	  if (isRationalType(other)) {
			return less(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}

	public IBool equal(INumber other) {
	  if (isRealType(other)) {
      return equal(other.toReal());
    }
    if (isIntegerType(other)) {
      return equal(other.toInteger());
    }
    if (isRationalType(other)) {
      return equal(other.toRational());
    }
    throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}
	
	public IBool lessEqual(INumber other) {
	  if (isRealType(other)) {
			return lessEqual(other.toReal());
		}
	  if (isIntegerType(other)) {
			return lessEqual(other.toInteger());
		}
		if (isRationalType(other)) {
			return lessEqual(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}

	public INumber multiply(INumber other) {
		if (isRealType(other)) {
			return multiply(other.toReal());
		}
		if (isIntegerType(other)) {
			return multiply(other.toInteger());
		}
		if (isRationalType(other)) {
			return multiply(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}

	public INumber subtract(INumber other) {
		if (isRealType(other)) {
			return subtract(other.toReal());
		}
		if (isIntegerType(other)) {
			return subtract(other.toInteger());
		}
		if (isRationalType(other)) {
			return subtract(other.toRational());
		}
		throw new UnexpectedTypeException(TypeFactory.getInstance().numberType(), other.getType());
	}
}
