package io.usethesource.treasure.exceptions;

import io.usethesource.treasure.type.Type;

public class UnexpectedConstructorTypeException extends UnexpectedTypeException {
	private static final long serialVersionUID = -6198133177142765746L;

	public UnexpectedConstructorTypeException(Type expected, Type got) {
		super(expected, got);
	}

}
