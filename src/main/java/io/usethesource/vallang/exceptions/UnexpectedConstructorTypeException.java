package io.usethesource.vallang.exceptions;

import io.usethesource.vallang.type.Type;

public class UnexpectedConstructorTypeException extends UnexpectedTypeException {
    private static final long serialVersionUID = -6198133177142765746L;

    public UnexpectedConstructorTypeException(Type expected, Type got) {
        super(expected, got);
    }

}
