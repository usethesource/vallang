package org.rascalmpl.value.exceptions;

import org.rascalmpl.value.type.Type;

public class UnexpectedElementTypeException extends UnexpectedTypeException {
	private static final long serialVersionUID = 8098855538349829776L;

	public UnexpectedElementTypeException(Type expected, Type got) {
		super(expected, got);
	}
}
