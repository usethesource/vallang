package io.usethesource.vallang.exceptions;

import io.usethesource.vallang.type.Type;

public class IllegalKeywordParameterDeclarationException extends FactTypeUseException {
    private static final long serialVersionUID = -1073149631907760703L;

    public IllegalKeywordParameterDeclarationException(Type type) {
        super("Keyword parameters can not be declared on " + type);
    }
}
