package io.usethesource.treasure.exceptions;

import io.usethesource.treasure.type.Type;

public class UnexpectedMapValueTypeException extends UnexpectedTypeException {
	private static final long serialVersionUID = -5746761186412867857L;

	public UnexpectedMapValueTypeException(Type expected, Type got) {
		super(expected, got);
	}

}
