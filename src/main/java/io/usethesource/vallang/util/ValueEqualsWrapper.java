package io.usethesource.vallang.util;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IValue;

/**
 * This class is necessary to implement the ignoring
 * of value annotations during hash-table lookups. Since
 * annotations are deprecated, this value wrapper is also 
 * a temporary solution.
 */
public class ValueEqualsWrapper {
    private final IValue value;
    
    public ValueEqualsWrapper(IValue value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (!(obj instanceof ValueEqualsWrapper)) {
            return false;
        }
        
        // equals defaults to isEqual which ignores annotations (recursively)
        return value.isEqual(((ValueEqualsWrapper) obj).value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public IValue getValue() {
        return value;
    }
}
