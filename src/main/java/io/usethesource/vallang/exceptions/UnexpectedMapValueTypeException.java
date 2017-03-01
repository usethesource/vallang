package io.usethesource.vallang.exceptions;

import io.usethesource.vallang.type.Type;

public class UnexpectedMapValueTypeException extends UnexpectedTypeException {
	private static final long serialVersionUID = -5746761186412867857L;

	public UnexpectedMapValueTypeException(Type expected, Type got) {
		super(expected, got);
	}

}
