package io.usethesource.vallang.exceptions;

public class TypeParseError extends RuntimeException {
    private static final long serialVersionUID = 7054359699049181437L;
    private int offset = -1;

    public TypeParseError(String message, Throwable cause) {
        super(message, cause);
    }
    
    public TypeParseError(String message, int offset) {
        super(message);
        this.offset = offset;
    }
    
    public TypeParseError(String message, int offset, Throwable cause) {
        super(message + " at offset " + offset, cause);
        this.offset = offset;
    }

    public boolean hasCause() {
        return getCause() != null;
    }
    
    
    public boolean hasOffset() {
        return offset != -1;
    }
    public int getOffset() {
        return offset;
    }
}
