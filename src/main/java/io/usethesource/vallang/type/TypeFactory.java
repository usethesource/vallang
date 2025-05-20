/*******************************************************************************
 * Copyright (c) 2007, 2008, 2012, 2015 IBM Corporation and CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com)
 *    Jurgen Vinju  (jurgen@vinju.org)
 *    Paul Klint (Paul.Klint@cwi.nl)
 *    Anya Helene Bagge (anya@ii.uib.no)
 *******************************************************************************/

package io.usethesource.vallang.type;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeDeclarationException;
import io.usethesource.vallang.exceptions.IllegalFieldNameException;
import io.usethesource.vallang.exceptions.IllegalFieldTypeException;
import io.usethesource.vallang.exceptions.IllegalIdentifierException;
import io.usethesource.vallang.exceptions.NullTypeException;
import io.usethesource.vallang.util.HashConsingMap;
import io.usethesource.vallang.util.WeakReferenceHashConsingMap;

/**
 * Use this class to produce any kind of {@link Type}, after which the make
 * methods of Type can be used in conjunction with a reference to an
 * {@link IValueFactory} to produce {@link IValue}'s of a certain type.
 * <p>
 *
 * @see {@link Type} and {@link IValueFactory} for more information.
 */
public class TypeFactory {
    /**
     * Caches all types to implement canonicalization
     */
    private final HashConsingMap<Type> fCache = new WeakReferenceHashConsingMap<>(32*1024, (int)TimeUnit.MINUTES.toSeconds(30));
    private volatile @MonotonicNonNull TypeValues typeValues; // lazy initialize

    private static class InstanceHolder {
        public static final TypeFactory sInstance = new TypeFactory();
    }

    private TypeFactory() { }

    public static TypeFactory getInstance() {
        return InstanceHolder.sInstance;
    }

    @Override
    public String toString() {
        return "TF";
    }

    public Type randomType(TypeStore store, RandomTypesConfig config) {
        return cachedTypeValues().randomType(store, config);
    }

    public Type randomADTType(TypeStore store, RandomTypesConfig config) {
        assert config.isWithRandomAbstractDatatypes();

        synchronized (this) {
            var tv = cachedTypeValues();
            return tv.getRandomADTType(store, config);
        }
    }

    public Type randomType(TypeStore typeStore) {
        return cachedTypeValues().randomType(typeStore, RandomTypesConfig.defaultConfig(new Random()));
    }

    public Type randomType(TypeStore typeStore, int depth) {
        return cachedTypeValues().randomType(typeStore, RandomTypesConfig.defaultConfig(new Random()).maxDepth(depth));
    }

    public Type randomType(TypeStore typeStore, Random r, int depth) {
        return cachedTypeValues().randomType(typeStore, RandomTypesConfig.defaultConfig(r).maxDepth(depth));
    }



    /**
     * construct the value type, which is the top type of the type hierarchy
     * representing all possible values.
     *
     * @return a unique reference to the value type
     */
    public Type valueType() {
        return ValueType.getInstance();
    }

    /**
     * construct the void type, which is the bottom of the type hierarchy,
     * representing no values at all.
     *
     * @return a unique reference to the void type
     */
    public Type voidType() {
        return VoidType.getInstance();
    }

    /*package*/ Type getFromCache(Type t) {
        return fCache.get(t);
    }

    /**
     * Construct a new type.
     *
     * @return a reference to the unique integer type of the PDB.
     */
    public Type integerType() {
        return IntegerType.getInstance();
    }

    /**
     * Construct a new type.
     *
     * @return a reference to the unique number type of the PDB.
     */
    public Type numberType() {
        return NumberType.getInstance();
    }

    /**
     * Construct a new type.
     *
     * @return a reference to the unique double type of the PDB.
     */
    public Type realType() {
        return RealType.getInstance();
    }

    /**
     * Construct a new type.
     *
     * @return a reference to the unique rational type of the PDB.
     */
    public Type rationalType() {
        return RationalType.getInstance();
    }

    /**
     * Construct a new bool type
     *
     * @return a reference to the unique boolean type of the PDB.
     */
    public Type boolType() {
        return BoolType.getInstance();
    }

    /**
     * Retrieves an older equal externalType from the cache, or stores a new
     * external type if it does not exist.
     *
     * @param externalType
     * @return an object equal to externalType but possibly older
     */
    public Type externalType(Type externalType) {
        assert !isNull(externalType) : "external type cannot be null";
        return getFromCache(externalType);
    }

    /**
     * Construct a new type.
     *
     * @return a reference to the unique string type of the PDB.
     */
    public Type stringType() {
        return StringType.getInstance();
    }

    /**
     * Construct a new type.
     *
     * @return a reference to the unique sourceLocation type of the PDB.
     */
    public Type sourceLocationType() {
        return SourceLocationType.getInstance();
    }

    /**
     * Construct a new type.
     *
     * @return a reference to the unique datetime type of the PDB.
     */
    public Type dateTimeType() {
        return DateTimeType.getInstance();
    }

    private TupleType getOrCreateTuple(Type[] fieldTypes) {
        return (TupleType) getFromCache(new TupleType(fieldTypes));
    }

    /**
     * Construct a tuple type.
     *
     * @return a reference to the unique empty tuple type.
     */
    public Type tupleEmpty() {
        return getFromCache(new TupleType(new Type[0]));
    }

    /**
     * Construct a tuple type. Note that if you pass an array, this array should
     * NEVER be modified after or serious breakage will occur.
     *
     * @param fieldTypes
     *          a list of field types in order of appearance.
     * @return a tuple type or `void` in case one of the field types was void
     */
    public Type tupleType(Type... fieldTypes) {
        assert !anyNull(fieldTypes) : "tuple field types should not be null";

        // tuple values with elements of type void (like tuple[void,void])
        // can not exist, so the type that represents that empty set of values is `void`,
        // canonically. Without this canonicalization, type instantiation of
        // types such as rel[&T, &Y] for empty sets would return rel[void, void] = set[tuple[void,void]]
        // instead of the correct `set[void]`, also pattern matching and equality would fail seemingly
        // randomly on empty relations depending on how they've been instantiated.
        for (Type elem : fieldTypes) {
            if (elem.isBottom()) {
                return elem;
            }
        }

        return getFromCache(new TupleType(fieldTypes));
    }

    /**
     * Construct a labeled tuple type. Note that if you pass an array, this array
     * should NEVER be modified after or serious breakage will occur.
     *
     * @param fieldTypesAndLabel
     *          an array of field types, where each field type of type @{link
     *          Type} is immediately followed by a field label of type @{link
     *          String}.
     * @return a tuple type
     * @throws FactTypeDeclarationException
     *           when one of the labels is not a proper identifier or when the
     *           argument array does not contain alternating types and labels.
     */
    @Deprecated
    public Type tupleType(Object... fieldTypesAndLabels) throws FactTypeDeclarationException {
        if (fieldTypesAndLabels.length == 0) {
            return tupleEmpty();
        }

        int N = fieldTypesAndLabels.length;
        int arity = N / 2;
        Type[] protoFieldTypes = new Type[arity];
        String[] protoFieldNames = new String[arity];
        for (int i = 0; i < N; i += 2) {
            int pos = i / 2;
            if (fieldTypesAndLabels[i] == null || fieldTypesAndLabels[i + 1] == null) {
                throw new NullPointerException();
            }
            try {
                protoFieldTypes[pos] = (Type) fieldTypesAndLabels[i];
                if (protoFieldTypes[pos].isBottom()) {
                    // if one of the fields is void, then the entire tuple could
                    // never be instantiated and thus its the void type (the empty set of
                    // values) that represents it canonically:
                    return voidType();
                }
            } catch (ClassCastException e) {
                throw new IllegalFieldTypeException(pos, fieldTypesAndLabels[i], e);
            }
            try {
                String name = (String) fieldTypesAndLabels[i + 1];
                if (!isIdentifier(name)) {
                    throw new IllegalIdentifierException(name);
                }
                protoFieldNames[pos] = name;
            } catch (ClassCastException e) {
                throw new IllegalFieldNameException(pos, fieldTypesAndLabels[i + 1], e);
            }
        }

        return getFromCache(new TupleTypeWithFieldNames(protoFieldTypes, protoFieldNames));
    }

    /**
     * Construct a tuple type. The length of the types and labels arrays should be
     * equal.
     *
     * @param types
     *          the types of the fields
     * @param labels
     *          the labels of the fields (in respective order)
     * @return a tuple type
     */
    @Deprecated
    public Type tupleType(Type[] types, String[] labels) {
        assert !anyNull(types) : "tuples types should not contain nulls";
        assert !anyNull(labels) : "label types should not contain nulls";
        assert types.length == labels.length;

        if (types.length == 0) {
            return tupleEmpty();
        }

        // if one of the elements is of type void, then a tuple can never
        // be instantiated of this type and thus void is the type to represent it:
        for (Type elem : types) {
            if (elem.isBottom()) {
                return voidType();
            }
        }

        return getFromCache(new TupleTypeWithFieldNames(types, labels));
    }

    /**
     * Construct a tuple type.
     *
     * @param fieldTypes
     *          an array of field types in order of appearance. The array is
     *          copied.
     * @return a tuple type
     */
    public Type tupleType(IValue... elements) {
        assert !anyNull(elements) : "tuple type elements should not contain any nulls";
        int N = elements.length;
        Type[] fieldTypes = new Type[N];
        for (int i = N - 1; i >= 0; i--) {
            fieldTypes[i] = elements[i].getType();
        }
        return getOrCreateTuple(fieldTypes);
    }

    /**
     * Construct a function type by composing it from a return type and two tuple types
     * for the arguments and the keyword parameters. These respective tuples must not
     * have had any void parameters, otherwise this function will throw an exception.
     *
     * @param returnType
     * @param argumentTypesTuple
     * @param keywordParameterTypesTuple
     * @return
     */

    public Type functionType(Type returnType, Type argumentTypesTuple, Type keywordParameterTypesTuple) {
        if (argumentTypesTuple.isBottom()) {
            throw new IllegalArgumentException("argumentTypes must be a proper tuple type (without any void fields)");
        }

        if (keywordParameterTypesTuple.isBottom()) {
            throw new IllegalArgumentException("keywordParameterTypes must be a tuple type (without any void fields)");
        }

        return getFromCache(new FunctionType(returnType, (TupleType) argumentTypesTuple,  (TupleType) keywordParameterTypesTuple));
    }

    /**
     * Construct a function type with labeled parameter and keyword field types
     */
    public Type functionType(Type returnType, Type[] argumentTypes, String[] argumentLabels, Type[] keywordParameterTypes, String[] keywordParameterLabels) {
        assert !anyNull(argumentTypes) : "argument types should not be null";
        assert !anyNull(argumentLabels) : "argument labels should not contain nulls";
        assert !anyNull(keywordParameterTypes) : "keyword parameter types should not contain nulls";
        assert !anyNull(keywordParameterLabels) : "keyword parameter types should not contain nulls";
        return getFromCache(new FunctionType(returnType,
            (TupleType) getFromCache(new TupleTypeWithFieldNames(argumentTypes, argumentLabels)),
            (TupleType) getFromCache(new TupleTypeWithFieldNames(keywordParameterTypes, keywordParameterLabels))
        ));
    }

    /**
     * Construct a function type with unlabeled parameter and keyword field types
     */
    public Type functionType(Type returnType, Type[] argumentTypes, Type[] keywordParameterTypes) {
        assert !anyNull(argumentTypes) : "argument types should not be null";
        assert !anyNull(keywordParameterTypes) : "keyword parameter types should not contain nulls";
        return getFromCache(new FunctionType(returnType,
            (TupleType) getFromCache(new TupleType(argumentTypes)),
            (TupleType) getFromCache(new TupleType(keywordParameterTypes))
        ));
    }

    /**
     * Construct a set type
     *
     * @param eltType
     *          the type of elements in the set
     * @return a set type
     */
    public Type setType(Type eltType) {
        assert !isNull(eltType) : "set type should not be null";
        return getFromCache(new SetType(eltType));
    }

    public Type relTypeFromTuple(Type tupleType) {
        assert !isNull(tupleType) : "rel type should not be null";
        return setType(tupleType);
    }

    public Type lrelTypeFromTuple(Type tupleType) {
        assert !isNull(tupleType) : "lrel type should not be null";

        return listType(tupleType);
    }

    /**
     * Construct a relation type.
     *
     * @param fieldTypes
     *          the types of the fields of the relation
     * @return a relation type
     */
    public Type relType(Type... fieldTypes) {
        assert !anyNull(fieldTypes) : "rel types should not contain nulls";
        return setType(tupleType(fieldTypes));
    }

    /**
     * Construct a relation type.
     *
     * @param fieldTypes
     *          the types of the fields of the relation
     * @return a relation type
     */
    @Deprecated
    public Type relType(Object... fieldTypesAndLabels) {
        return setType(tupleType(fieldTypesAndLabels));
    }

    /**
     * Construct a list relation type.
     *
     * @param fieldTypes
     *          the types of the fields of the relation
     * @return a list relation type
     */
    public Type lrelType(Type... fieldTypes) {
        assert !anyNull(fieldTypes) : "lrel types should not contain nulls";
        return listType(tupleType(fieldTypes));
    }

    /**
     * Construct a list relation type.
     *
     * @param fieldTypes
     *          the types of the fields of the relation
     * @return a list relation type
     */
    @Deprecated
    public Type lrelType(Object... fieldTypesAndLabels) {
        return lrelTypeFromTuple(tupleType(fieldTypesAndLabels));
    }

    /**
     * Construct an alias type. The alias may be parameterized to make an abstract
     * alias. Each ParameterType embedded in the aliased type should occur in the
     * list of parameters.
     *
     * @param store
     *          to store the declared alias in
     * @param name
     *          the name of the type
     * @param aliased
     *          the type it should be an alias for
     * @param parameters
     *          a list of type parameters for this alias
     * @return an alias type
     * @throws FactTypeDeclarationException
     *           if a type with the same name but a different supertype was
     *           defined earlier as a named type of a AbstractDataType.
     */
    public Type aliasType(TypeStore store, String name, Type aliased, Type... parameters)
            throws FactTypeDeclarationException {
        assert !isNull(store) : "alias store should not be null";
        assert !isNull(name) : "alias name should not be null";
        assert !isNull(aliased) : "alias aliased should not be null";
        assert !anyNull(parameters): "alias parameters should not be null";

        Type paramType;
        if (parameters.length == 0) {
            paramType = voidType();
        } else {
            paramType = tupleType(parameters);
        }

        return aliasTypeFromTuple(store, name, aliased, paramType);
    }

    public Type aliasTypeFromTuple(TypeStore store, String name, Type aliased, Type params)
            throws FactTypeDeclarationException {
        assert !isNull(store) : "alias store should not be null";
        assert !isNull(name) : "alias name should not be null";
        assert !isNull(aliased) : "alias aliased should not be null";
        assert !isNull(params): "alias params should not be null";

        if (!isIdentifier(name)) {
            throw new IllegalIdentifierException(name);
        }

        if (aliased == null) {
            throw new NullTypeException();
        }

        Type result = getFromCache(new AliasType(name, aliased, params));
        store.declareAlias(result);
        return result;
    }

    public Type nodeType() {
        return NodeType.getInstance();
    }

    /**
     * Construct a @{link AbstractDataType}, which is a kind of tree node. Each
     * kind of tree node may have different alternatives, which are
     * ConstructorTypes. A @{link ConstructorType} is always a sub-type of a
     * AbstractDataType. A AbstractDataType is always a sub type of value.
     *
     * @param store
     *          to store the declared adt in
     * @param name
     *          the name of the abstract data-type
     * @param parameters
     *          array of type parameters
     * @return a AbstractDataType
     * @throws IllegalIdentifierException
     */
    public Type abstractDataType(TypeStore store, String name, Type... parameters) throws FactTypeDeclarationException {
        assert !isNull(store) : "adt store should not be null";
        assert !isNull(name) : "adt name should not be null";
        assert !anyNull(parameters) : "adt parameters should not contain a null";

        Type paramType = voidType();
        if (parameters.length != 0) {
            paramType = tupleType(parameters);
        }

        return abstractDataTypeFromTuple(store, name, paramType);
    }

    public Type abstractDataTypeFromTuple(TypeStore store, String name, Type params) throws FactTypeDeclarationException {
        assert !isNull(store) : "adt store should not be null";
        assert !isNull(name) : "adt name should not be null";
        assert !isNull(params): "adt params should not be null";

        if (!isIdentifier(name)) {
            throw new IllegalIdentifierException(name);
        }
        Type result = getFromCache(new AbstractDataType(name, params));

        if (!params.equivalent(voidType()) && params.getArity() > 0) {
            if (params.getFieldType(0).isOpen()) { // parametrized and uninstantiated
                // adts should be stored
                store.declareAbstractDataType(result);
            }
        } else { // not parametrized
            store.declareAbstractDataType(result);
        }

        return result;
    }

    /**
     * Make a new constructor type. A constructor type extends an abstract data
     * type such that it represents more values.
     *
     * @param store
     *          to store the declared constructor in
     * @param adt
     *          the AbstractDataType this constructor builds
     * @param name
     *          the name of the node type
     * @param children
     *          the types of the children of the tree node type
     * @return a tree node type
     * @throws IllegalIdentifierException
     *           , UndeclaredAbstractDataTypeException,
     *           RedeclaredFieldNameException, RedeclaredConstructorException
     */
    public Type constructorFromTuple(TypeStore store, Type adt, String name, Type tupleType) throws FactTypeDeclarationException {
        assert !isNull(store) : "constructor store should not be null";
        assert !isNull(adt): "constructor adt should not be null";
        assert !isNull(name) : "constructor name should not be null";
        assert !isNull(tupleType) : "constructor type should not be null";

        if (!isIdentifier(name)) {
            throw new IllegalIdentifierException(name);
        }

        Type result = getFromCache(new ConstructorType(name, tupleType, adt));

        Type params = adt.getTypeParameters();

        if (!params.equivalent(voidType())) {
            if (params.isOpen()) { // only parametrized and not instantiated types
                // should be stored
                store.declareConstructor(result);
            }
        } else {
            store.declareConstructor(result);
        }

        return result;
    }

    /**
     * Make a new constructor type. A constructor type extends an abstract data
     * type such that it represents more values. Note that if you pass an array
     * for the children parameter, this array should NEVER be modified after or
     * serious breakage will occur.
     *
     * @param store
     *          to store the declared constructor in
     * @param adt
     *          the adt this constructor builds
     * @param name
     *          the name of the node type
     * @param children
     *          the types of the children of the tree node type
     * @return a tree node type
     */
    public Type constructor(TypeStore store, Type adt, String name, Type... children) throws FactTypeDeclarationException {
        return constructorFromTuple(store, adt, name, tupleType(children));
    }

    /**
     * Make a new constructor type. A constructor type extends an abstract data
     * type such that it represents more values.
     *
     * @param store
     *          to store the declared constructor in
     * @param nodeType
     *          the type of node this constructor builds
     * @param name
     *          the name of the node type
     * @param children
     *          the types of the children of the tree node type
     * @return a tree node type
     */
    public Type constructor(TypeStore store, Type nodeType, String name, Object... childrenAndLabels)
            throws FactTypeDeclarationException {
        return constructorFromTuple(store, nodeType, name, tupleType(childrenAndLabels));
    }

    /**
     * Construct a list type
     *
     * @param elementType
     *          the type of the elements in the list
     * @return a list type
     */
    public Type listType(Type elementType) {
        assert !isNull(elementType) : "list type should not be null";
        return getFromCache(new ListType(elementType));
    }

    /**
     * Construct a map type
     *
     * @param key
     *          the type of the keys in the map
     * @param value
     *          the type of the values in the map
     * @return a map type
     */
    public Type mapType(Type key, Type value) {
        assert !isNull(key) : "map key type should not be null";
        assert !isNull(value) : "map key type should not be null";
        return getFromCache(new MapType(key, value));
    }

    public Type mapTypeFromTuple(Type fields) {
        assert !isNull(fields) : "map tuple type should not be null";

        if (fields.isBottom()) {
            return mapType(voidType(), voidType());
        }

        if (!fields.isFixedWidth()) {
            throw new UnsupportedOperationException("fields argument should be a tuple. not " + fields);
        }
        if (fields.getArity() < 2) {
            throw new IndexOutOfBoundsException();
        }
        if (fields.hasFieldNames()) {
            return mapType(fields.getFieldType(0), Objects.requireNonNull(fields.getFieldName(0)), fields.getFieldType(1), Objects.requireNonNull(fields.getFieldName(1)));
        } else {
            return mapType(fields.getFieldType(0), fields.getFieldType(1));
        }
    }

    public Type mapType(Type key, String keyLabel, Type value, String valueLabel) {
        assert !isNull(key) : "map key type should not be null";
        assert !isNull(value) : "map key type should not be null";
        if ((keyLabel != null && valueLabel == null) || (valueLabel != null && keyLabel == null)) {
            throw new IllegalArgumentException("Key and value labels must both be non-null or null: " + keyLabel + ", "
                    + valueLabel);
        }
        return getFromCache(new MapTypeWithFieldNames(key, keyLabel, value, valueLabel));
    }

    /**
     * Construct a type parameter, which can later be instantiated.
     *
     * @param name
     *          the name of the type parameter
     * @param bound
     *          the widest type that is acceptable when this type is instantiated
     * @return a parameter type
     */
    public Type parameterType(String name, Type bound) {
        assert !isNull(name) : "parameter name should not be null";
        assert !isNull(bound) : "parameter type should not be null";
        return getFromCache(new ParameterType(name, bound));
    }

    /**
     * Reconstruct a type from its symbolic value representation, and if relevant add
     * its declaration to the store (for ADTs, constructors and aliases)
     *
     * @param symbol  value representation of a type
     * @param store   store to put declarations in
     * @param grammar function to look up definitions for non-terminal types
     * @return a type isomorphic to the given symbolic representation
     */
    public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar) {
        return cachedTypeValues().fromSymbol(symbol, store, grammar);
    }

    /**
     * Represent this type as a value of the abstract data-type "Symbol". As a side-effect
     * it will also add Production values to the grammar map, including all necessary productions
     * to build values of the receiver type, transitively.
     *
     * @param  vf valuefactory to use
     * @param store store to lookup additional necessary definitions in to store in the grammar
     * @param grammar map to store production values in as a side-effect
     * @return a value to uniquely represent this type.
     */
    public IConstructor asSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar) {
        return type.asSymbol(vf, store, grammar, new HashSet<>());
    }

    /**
     * Parsers the serialized representation of types (what is produced by Type.toString())
     * @param Reader reader
     * @return
     * @throws IOException
     */
    public Type fromString(TypeStore store, Reader reader) throws IOException {
        return new TypeReader().read(store, reader);
    }

    /**
     * Construct a type parameter, which can later be instantiated.
     *
     * @param name
     *          the name of the type parameter
     * @return a parameter type
     */
    public Type parameterType(String name) {
        assert !isNull(name) : "parameter name should not be null";
        return getFromCache(new ParameterType(name));
    }

    private boolean anyNull(@Nullable Object[] os) {
        for (@Nullable Object o: os) {
            if (isNull(o)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNull(@Nullable Object os) {
        return Objects.isNull(os);
    }

    /**
     * Checks to see if a string is a valid PDB type, or field name
     * identifier
     *
     * @param str
     * @return true if the string is a valid identifier
     */
    public boolean isIdentifier(String str) {
        assert !isNull(str) : "str should not be null";

        int len = str.length();
        if (len == 0) {
            return false;
        }

        if (!Character.isJavaIdentifierStart(str.charAt(0))) {
            return false;
        }

        for (int i = len - 1; i > 0; i--) {
            char c = str.charAt(i);
            if (!(Character.isJavaIdentifierPart(c) || c == '.' || c == '-')) {
                return false;
            }
        }

        return true;
    }

    public abstract static class TypeReifier {
        private final TypeValues cachedSymbols;

        public TypeReifier(TypeValues symbols) {
            this.cachedSymbols = symbols;
        }

        public TypeValues symbols() {
            return cachedSymbols;
        }

        public TypeFactory tf() {
            return TypeFactory.getInstance();
        }


        public Set<Type> getSymbolConstructorTypes() {
            return Collections.singleton(getSymbolConstructorType());
        }

        public abstract Type getSymbolConstructorType();

        public boolean isRecursive() {
            return false;
        }

        public abstract Type randomInstance(BiFunction<TypeStore, RandomTypesConfig, Type> next, TypeStore store, RandomTypesConfig rnd);

        public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
            // this will work for all nullary type symbols with only one constructor type
            return vf.constructor(getSymbolConstructorType());
        }

        public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
            // normally nothing
        }

        public abstract Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar);

        public String randomLabel(RandomTypesConfig rnd) {
            return "x" + new BigInteger(32, rnd.getRandom()).toString(32);
        }

        public Type randomTuple(BiFunction<TypeStore, RandomTypesConfig,Type> next, TypeStore store, RandomTypesConfig rnd) {
            return new TupleType.Info(cachedSymbols).randomInstance(next, store, rnd);
        }

        public Type randomTuple(BiFunction<TypeStore, RandomTypesConfig,Type> next, TypeStore store, RandomTypesConfig rnd, int arity) {
            return new TupleType.Info(cachedSymbols).randomInstance(next, store, rnd, arity);
        }
    }

    public static class RandomTypesConfig {
        private final Random random;
        private final int maxDepth;
        private final boolean withTypeParameters;
        private final boolean withAliases;
        private final boolean withTupleFieldNames;
        private final boolean withMapFieldNames;
        private final boolean withRandomAbstractDatatypes;

        private RandomTypesConfig(Random random) {
            this.random = random;
            this.maxDepth = 5;
            this.withAliases = false;
            this.withTupleFieldNames = false;
            this.withTypeParameters = false;
            this.withMapFieldNames = false;
            this.withRandomAbstractDatatypes = true;
        }

        private RandomTypesConfig(Random random, int maxDepth, boolean withTypeParameters, boolean withAliases, boolean withTupleFieldNames, boolean withRandomAbstractDatatypes, boolean withMapFieldNames) {
            this.random = random;
            this.maxDepth = maxDepth;
            this.withAliases = withAliases;
            this.withTupleFieldNames = withTupleFieldNames;
            this.withTypeParameters = withTypeParameters;
            this.withRandomAbstractDatatypes = withRandomAbstractDatatypes;
            this.withMapFieldNames = withMapFieldNames;
        }

        public static RandomTypesConfig defaultConfig(Random random) {
            return new RandomTypesConfig(random);
        }

        public Random getRandom() {
            return random;
        }

        public boolean nextBoolean() {
            return random.nextBoolean();
        }

        public int nextInt(int bound) {
            return random.nextInt(bound);
        }

        public boolean isWithAliases() {
            return withAliases;
        }

        public boolean isWithTupleFieldNames() {
            return withTupleFieldNames;
        }

        public boolean isWithMapFieldNames() {
            return withMapFieldNames;
        }

        public boolean isWithTypeParameters() {
            return withTypeParameters;
        }

        public boolean isWithRandomAbstractDatatypes() {
            return withRandomAbstractDatatypes;
        }

        public int getMaxDepth() {
            return maxDepth;
        }

        public RandomTypesConfig maxDepth(int newMaxDepth) {
            return new RandomTypesConfig(random, newMaxDepth, withTypeParameters, withAliases, withTupleFieldNames, withRandomAbstractDatatypes, withMapFieldNames);
        }

        public RandomTypesConfig withAliases() {
            return new RandomTypesConfig(random, maxDepth, withTypeParameters, true, withTupleFieldNames, withRandomAbstractDatatypes, withMapFieldNames);
        }

        public RandomTypesConfig withTypeParameters() {
            return new RandomTypesConfig(random, maxDepth, true, withAliases, withTupleFieldNames, withRandomAbstractDatatypes, withMapFieldNames);
        }

        public RandomTypesConfig withTupleFieldNames() {
            return new RandomTypesConfig(random, maxDepth, withTypeParameters, withAliases, true, withRandomAbstractDatatypes, withMapFieldNames);
        }

        public RandomTypesConfig withoutRandomAbstractDatatypes() {
            return new RandomTypesConfig(random, maxDepth, withTypeParameters, withAliases, withTupleFieldNames, false, withMapFieldNames);
        }

        public RandomTypesConfig withMapFieldNames() {
            return new RandomTypesConfig(random, maxDepth, withTypeParameters, withAliases, withTupleFieldNames, withRandomAbstractDatatypes, true);
        }
    }

    public class TypeValues {
        private static final String TYPES_CONFIG = "io/usethesource/vallang/type/types.config";

        private final TypeStore symbolStore = new TypeStore();
        private final Type Symbol = abstractDataType(symbolStore, "Symbol");
        private final Type Symbol_Label = constructor(symbolStore, Symbol, "label", stringType(), "name", Symbol, "symbol");

        private final Map<Type, TypeReifier> symbolConstructorTypes = new ConcurrentHashMap<>();

        private TypeValues() {  }

        public Type randomType(TypeStore store, RandomTypesConfig config) {
            BiFunction<TypeStore,RandomTypesConfig,Type> next = new BiFunction<TypeStore,RandomTypesConfig,Type>() {
                int maxTries = config.getMaxDepth();

                @Override
                public Type apply(TypeStore store, RandomTypesConfig rnd) {
                    if (maxTries-- > 0) {
                        return getRandomType(this, store, rnd);
                    }
                    else {
                        return getRandomNonRecursiveType(this, store, rnd);
                    }
                }
            };

            return config.getMaxDepth() > 0 ? getRandomType(next, store, config) : getRandomNonRecursiveType(next, store, config);
        }

        private Type getRandomNonRecursiveType(BiFunction<TypeStore,RandomTypesConfig,Type> next, TypeStore store, RandomTypesConfig rnd) {
            Iterator<TypeReifier> it = symbolConstructorTypes.values().iterator();
            TypeReifier reifier = it.next();

            while (it.hasNext()) {
                reifier = it.next();
                if (!reifier.isRecursive()) {
                    break;
                }
            }

            assert !reifier.isRecursive()
                :  "a recursive type could only happen here if no non-recursive types has been registered at all.";

            return reifier.randomInstance(next, store, rnd);
        }

        private Type getRandomType(BiFunction<TypeStore,RandomTypesConfig,Type> next, TypeStore store, RandomTypesConfig rnd) {
            TypeReifier[] alts = symbolConstructorTypes.values().toArray(new TypeReifier[0]);
            TypeReifier selected = alts[Math.max(0, alts.length > 0 ? rnd.nextInt(alts.length) - 1 : 0)];

            // increase the chance of recursive types
            while (rnd.nextBoolean() && !selected.isRecursive()) {
                selected = alts[Math.max(0, rnd.nextInt(alts.length))];
            }

            return selected.randomInstance(next, store, rnd);
        }

        public Type getRandomADTType(TypeStore store, RandomTypesConfig rnd) {
            BiFunction<TypeStore,RandomTypesConfig,Type> next = new BiFunction<TypeStore,RandomTypesConfig,Type>() {
                int maxTries = rnd.getMaxDepth();

                @Override
                public Type apply(TypeStore store, RandomTypesConfig rnd) {
                    if (maxTries-- > 0) {
                        return getRandomType(this, store, rnd);
                    }
                    else {
                        return getRandomNonRecursiveType(this, store, rnd);
                    }
                }
            };

            return symbolConstructorTypes
                .values()
                .stream()
                .filter(p -> p instanceof AbstractDataType.Info)
                .findFirst()
                .get()
                .randomInstance(next, store, rnd);
        }

        public boolean isLabel(IConstructor symbol) {
            return symbol.getConstructorType() == Symbol_Label;
        }

        public String getLabel(IValue symbol) {
            assert symbol instanceof IConstructor && isLabel((IConstructor) symbol);
            return ((IString) ((IConstructor) symbol).get("name")).getValue();
        }

        public IConstructor labelSymbol(IValueFactory vf, IConstructor symbol, String label) {
            return vf.constructor(Symbol_Label, vf.string(label), symbol);
        }

        public IConstructor getLabeledSymbol(IValue symbol) {
            assert symbol instanceof IConstructor && isLabel((IConstructor) symbol);
            return (IConstructor) ((IConstructor) symbol).get("symbol");
        }

        public Type typeSymbolConstructor(String name, Object... args) {
            return constructor(symbolStore, symbolADT(), name, args);
        }

        public Type typeProductionConstructor(String name, Object... args) {
            return constructor(symbolStore, productionADT(), name, args);
        }

        public Type symbolADT() {
            return abstractDataType(symbolStore, "Symbol");
        }

        public Type attrADT() {
            return abstractDataType(symbolStore, "Attr");
        }

        public Type productionADT() {
            return abstractDataType(symbolStore, "Production");
        }

        public void initialize() {
            try {
                Enumeration<URL> resources = checkValidClassLoader(getClass().getClassLoader()).getResources(TYPES_CONFIG);
                Collections.list(resources).forEach(f -> loadServices(f));
            } catch (IOException e) {
                throw new Error("WARNING: Could not load type kind definitions from " + TYPES_CONFIG, e);
            }
        }

        private ClassLoader checkValidClassLoader(@Nullable ClassLoader cl) {
            if (cl == null) {
                throw new Error("Could not find class loader due to bootloader loading of this class");
            }
            return cl;
        }

        private void loadServices(URL nextElement) {
            try {
                for (String name : readConfigFile(nextElement)) {
                    name = name.trim();

                    if (name.startsWith("#") || name.isEmpty()) {
                        // source code comment
                        continue;
                    }

                    Class<?> clazz = checkValidClassLoader(Thread.currentThread().getContextClassLoader()).loadClass(name);
                    Object instance = clazz.getConstructor(TypeValues.class).newInstance(this);

                    if (instance instanceof TypeReifier) {
                        registerTypeInfo((TypeReifier) instance);
                    }
                    else {
                        throw new IllegalArgumentException("WARNING: could not load type info " + name + " because it does not implement TypeFactory.TypeInfo");
                    }
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException | IllegalArgumentException | SecurityException | IOException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalArgumentException("WARNING: could not load type info " + nextElement + " due to " + e.getMessage());
            }
        }

        private void registerTypeInfo(TypeReifier instance) {
            instance.getSymbolConstructorTypes().forEach(x -> symbolConstructorTypes.put(x, instance));
        }

        private String[] readConfigFile(URL nextElement) throws IOException {
            try (Reader in = new InputStreamReader(nextElement.openStream())) {
                StringBuilder res = new StringBuilder();
                char[] chunk = new char[1024];
                int read;
                while ((read = in.read(chunk, 0, chunk.length)) != -1) {
                    res.append(chunk, 0, read);
                }
                return res.toString().split("\n");
            }
        }

        /**
         * Converts a value representing a type back to a type and as a side-effect declares all necessary
         * data-types and constructors in the provided typestore.
         *
         * @param symbol is a constructor generated earlier by Type.asSymbol
         * @param store  is the typestore to store ADTs, constructors and kw fields in.
         * @param grammar is a lookup function to produce definitions for the types to store in the typestore
         * @return the type represented by the value
         */
        public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar) {
            TypeReifier reifier = symbolConstructorTypes.get(symbol.getConstructorType());

            if (reifier != null) {
                return reifier.fromSymbol(symbol, store, grammar);
            }

            throw new IllegalArgumentException("trying to construct a type from an unsupported type symbol: " + symbol + ", with this representation: " + symbol.getConstructorType());
        }

        /**
         * Builds a tuple type from a list of reified type symbols (see fromSymbol)
         */
        public Type fromSymbols(IList symbols, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar) {
            boolean allLabels = true;
            Type[] types = new Type[symbols.length()];
            String[] labels = new String[symbols.length()];

            for (int i = 0; i < symbols.length(); i++) {
                IConstructor elem = (IConstructor) symbols.get(i);
                if (elem.getConstructorType() == Symbol_Label) {
                    labels[i] = ((IString) elem.get("name")).getValue();
                    elem = (IConstructor) elem.get("symbol");
                }
                else {
                    allLabels = false;
                }

                types[i] = fromSymbol(elem, store, grammar);
            }

            if (allLabels) {
                return tupleType(types, labels);
            }
            else {
                return tupleType(types);
            }
        }
    }

    /**
     * Converts a value representing a type back to a type.
     * @param symbol is a constructor generated earlier by Type.asSymbol
     * @return the type represented by the value
     */
    public Type fromSymbol(IConstructor symbol) {
        return cachedTypeValues().fromSymbol(symbol, new TypeStore(), x -> Collections.emptySet());
    }

    public TypeValues cachedTypeValues() {
        var result = typeValues;
        if (result == null) {
            synchronized(this) {
                result = typeValues;
                if (result == null) {
                    result = new TypeValues();
                    result.initialize();
                    typeValues = result;
                }
            }
        }
        return result;
    }
}
