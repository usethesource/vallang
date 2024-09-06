package io.usethesource.vallang.exceptions;

import io.usethesource.vallang.type.Type;

public class OverloadingNotSupportedException extends FactTypeUseException {
    private static final long serialVersionUID = 5645367130014687132L;

    public OverloadingNotSupportedException(String constructorId) {
        super("Overloading is not supported (" + constructorId + ")");
    }
    
    public OverloadingNotSupportedException(Type adt, String constructorId) {
        super("Overloading is not supported (" + adt + "." + constructorId + ")");
    }

}
