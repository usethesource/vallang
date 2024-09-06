package io.usethesource.vallang.exceptions;

import io.usethesource.vallang.type.Type;

public class IllegalOperationException extends FactTypeUseException {
    private static final long serialVersionUID = 546504861606007094L;

    public IllegalOperationException(String op, Type lhs) {
        super("Operation " + op + " not allowed on " + lhs);
    }

    public IllegalOperationException(String op, Type lhs, Type rhs) {
        super("Operation " +op + " not allowed on " + lhs + " and " + rhs);
    }
}
