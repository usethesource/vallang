package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class UnexpectedChildTypeException extends UnexpectedTypeException {
	private static final long serialVersionUID = -1848764011952028440L;

	public UnexpectedChildTypeException(Type expected, Type got) {
		super(expected, got);
	}
}
