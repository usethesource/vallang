package io.usethesource.vallang.exceptions;

import org.checkerframework.checker.nullness.qual.Nullable;

public class IllegalFieldNameException extends FactTypeDeclarationException {
    private static final long serialVersionUID = -2480224409679761754L;
    private int pos;
    private Object elem;

    public IllegalFieldNameException(int pos, Object elem, ClassCastException cause) {
        super("Expected a field name at position " + pos + ", but got something different", cause);
        this.pos = pos;
        this.elem = elem;
    }

    public Object getElement() {
        return elem;
    }

    public int getPos() {
        return pos;
    }

    public synchronized @Nullable ClassCastException getCause() {
        return (ClassCastException) super.getCause();
    }
}
