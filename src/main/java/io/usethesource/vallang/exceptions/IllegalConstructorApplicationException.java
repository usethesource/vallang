package io.usethesource.vallang.exceptions;

import io.usethesource.vallang.type.Type;

public class IllegalConstructorApplicationException extends FactTypeUseException {
    private static final long serialVersionUID = -1412303012184333060L;

    public IllegalConstructorApplicationException(Type constructorType, Type arguments) {
        super("Constructor " + constructorType + " is not applicable to " + arguments);
    }


}
