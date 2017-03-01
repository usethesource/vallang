package io.usethesource.vallang.exceptions;

import io.usethesource.vallang.type.Type;

public class UnexpectedAnnotationTypeException extends UnexpectedTypeException {
	private static final long serialVersionUID = -4865168232421987847L;

	public UnexpectedAnnotationTypeException(Type expected, Type got) {
		super(expected, got);
	}

}
