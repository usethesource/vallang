package io.usethesource.treasure.exceptions;

import io.usethesource.treasure.type.Type;

public class UnexpectedChildTypeException extends UnexpectedTypeException {
	private static final long serialVersionUID = -1848764011952028440L;

	public UnexpectedChildTypeException(Type expected, Type got) {
		super(expected, got);
	}
}
