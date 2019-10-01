package io.usethesource.vallang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface TypeConfig {
    public enum Option {
        TYPE_PARAMETERS,
        ALIASES,
        TUPLE_FIELDNAMES,
        ALL
    }
    
    Option[] value();
}
