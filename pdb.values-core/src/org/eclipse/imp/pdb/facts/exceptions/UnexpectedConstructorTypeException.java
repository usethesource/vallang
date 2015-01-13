package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class UnexpectedConstructorTypeException extends UnexpectedTypeException {
	private static final long serialVersionUID = -6198133177142765746L;

	public UnexpectedConstructorTypeException(Type expected, Type got) {
		super(expected, got);
	}

}
