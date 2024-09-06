package io.usethesource.vallang.basic;

import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public class BooleanStoreProvider {

    public static TypeStore store = new TypeStore();
    private static TypeFactory tf = TypeFactory.getInstance();
    protected static Type Boolean = tf.abstractDataType(store, "Boolean");

    protected static Type Name = tf.abstractDataType(store, "Name");
    protected static Type True = tf.constructor(store, Boolean, "true");

    protected static Type False = tf.constructor(store, Boolean, "false");
    protected static Type And = tf.constructor(store, Boolean, "and", Boolean, Boolean);
    protected static Type Or = tf.constructor(store, Boolean, "or", tf.listType(Boolean));
    protected static Type Not = tf.constructor(store, Boolean, "not", Boolean);
    protected static Type TwoTups = tf.constructor(store, Boolean, "twotups",
        tf.tupleType(Boolean, Boolean), tf.tupleType(Boolean, Boolean));
    protected static Type NameNode = tf.constructor(store, Name, "name", tf.stringType());
    protected static Type Friends = tf.constructor(store, Boolean, "friends", tf.listType(Name));
    protected static Type Couples = tf.constructor(store, Boolean, "couples", tf.lrelType(Name, Name));
}
