/*******************************************************************************
* Copyright (c) 2009 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju  (jurgen@vinju.org)
*******************************************************************************/

package io.usethesource.vallang.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeDeclarationException;
import io.usethesource.vallang.exceptions.FactTypeRedeclaredException;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.exceptions.IllegalIdentifierException;
import io.usethesource.vallang.exceptions.IllegalKeywordParameterDeclarationException;
import io.usethesource.vallang.exceptions.RedeclaredConstructorException;
import io.usethesource.vallang.exceptions.RedeclaredFieldNameException;
import io.usethesource.vallang.exceptions.RedeclaredKeywordParameterException;
import io.usethesource.vallang.exceptions.UndeclaredAbstractDataTypeException;

/**
* This class manages type declarations. It stores declarations of keyword fields,
* type aliases and abstract data-type constructors.
* TypeStores can import others, but the imports are not transitive.
* Cyclic imports are allowed.
* <p>
* @see {@link TypeFactory}, {@link Type} and {@link IValueFactory} for more information.
*/
public class TypeStore {
    private final TypeFactory factory = TypeFactory.getInstance();

    private final Map<String, Type> fAliases= new HashMap<>();
    private final Map<String, Type> fADTs= new HashMap<>();
    private final Map<Type, Set<Type>> fConstructors = new HashMap<>();
    private final Map<Type, Map<String, Type>> fkeywordParameters = new HashMap<>();
    private final Set<TypeStore> fImports = new HashSet<>();

    /*
    * The ADTs for which overloading checking is turned off
    * (they play a role in the Rascal bootstrap procedure)
    */
    private static final java.util.List<String> IGNORE_OVERLOADING_CHECKS = Arrays.asList(
        new String[] {"AType", "Grammar", "RuntimeException", "ModuleStatus"});

    /**
    * A type store that is initially empty and imports the given TypeStores.
    * Note that imports are not transitive.
    */
    public TypeStore(TypeStore... imports) {
        for (TypeStore s : imports) {
            fImports.add(s);
        }
    }

    @Override
    public String toString() {
        return "TypeStore(adts=" + fADTs.size() + ",imports=" + fImports.size() + ")";
    }

    /**
    * Retrieves all ADT's declared in this TypeStore. Note that it does
    * not return the ADT's of imported TypeStores.
    */
    public Collection<Type> getAbstractDataTypes() {
        return Collections.unmodifiableCollection(fADTs.values());
    }

    /**
    * Retrieves all aliases declared in this TypeStore. Note that it does
    * not return the aliases of imported TypeStores.
    */
    public Collection<Type> getAliases() {
        return Collections.unmodifiableCollection(fAliases.values());
    }

    /**
    * Retrieves all keyword parameters declared in this TypeStore. Note that it does
    * not return the keyword parameters of imported TypeStores.
    *
    * @return a map of types for which keyword parameters are declared to a map of names of these
    * keyword parameters to the types of the values that give access to these keyword parameters.
    */
    public Map<Type, Map<String, Type>> getKeywordParameters() {
        Map<Type, Map<String,Type>> unmodifiableMap = new HashMap<>();
        for (Type key : fkeywordParameters.keySet()) {
            unmodifiableMap.put(key, Collections.unmodifiableMap(fkeywordParameters.get(key)));
        }
        return unmodifiableMap;
    }

    /**
    * Retrieves all constructors declared in this TypeStore. Note that it does
    * not return the constructors of imported TypeStores.
    */
    public Collection<Type> getConstructors() {
        Set<Type> result = new HashSet<>();
        for (Set<Type> adt : fConstructors.values()) {
            result.addAll(adt);
        }
        return Collections.unmodifiableCollection(result);
    }

    /**
    * Retrieves all imports declared in this TypeStore. Note that it does
    * not return the imports of imported TypeStores.
    */
    public Collection<TypeStore> getImports() {
        return Collections.unmodifiableCollection(fImports);
    }

    /**
    * Add other stores to the set of imported stores.
    * Note that imports are not transitive.
    *
    * @param stores
    */
    public void importStore(TypeStore... stores) {
        synchronized(fImports) {
            for (TypeStore s : stores) {
                doImport(s);
            }
        }
    }

    /**
    * Removes a number of stores from the imported stores. The stores to be removed must be reference-equal to some of
    * the stores imported by the receiver.
    *
    * @param stores to be removed
    */
    public final void unimportStores(TypeStore... stores) {
        synchronized (fImports) {
            fImports.removeAll(Arrays.asList(stores));
        }
    }

    private void doImport(TypeStore s) {
        checkOverlappingAliases(s);
        checkConstructorOverloading(s);

        fImports.add(s);
    }

    /**
    * Blindly copy all declaration of the other store into the receiver.
    */
    public void extendStore(TypeStore other) {
        synchronized (fAliases) {
            fAliases.putAll(other.fAliases);
        }

        synchronized (fADTs) {
            fADTs.putAll(other.fADTs);
        }

        synchronized (fConstructors) {
            for (Type type : other.fConstructors.keySet()) {
                Set<Type> set = fConstructors.get(type);

                if (set == null) {
                    set = new HashSet<>();
                }

                set.addAll(other.fConstructors.get(type));
                fConstructors.put(type, set);
            }
        }

        synchronized (fkeywordParameters) {
            fkeywordParameters.putAll(other.fkeywordParameters);
        }

        synchronized (fImports) {
            fImports.addAll(other.fImports);
        }
    }

    private void checkConstructorOverloading(TypeStore s) {
        for (Type type : fADTs.values()) {
            if(IGNORE_OVERLOADING_CHECKS.contains(type.getName())) { continue; }
            Type other = s.fADTs.get(type.getName());
            if (other != null && other == type) {
                Set<Type> signature1 = fConstructors.get(type);
                Set<Type> signature2 = s.fConstructors.get(type);

                if (signature2 == null || signature1 == null) {
                    continue; // nothing to check
                }

                for (Type alt : signature2) {
                    Type children = alt.getFieldTypes();
                    checkOverloading(signature1, alt.getName(), children);
                    try {
                        checkFieldNames(signature1, children);
                    }
                    catch (RedeclaredFieldNameException e) {
                        throw new RedeclaredFieldNameException(e.getFieldName(),
                        e.getFirstType(), e.getSecondType(),
                        type);
                    }
                }

            }
        }
    }

    private void checkOverlappingAliases(TypeStore s) {
        synchronized (fAliases) {
            for (Type alias : fAliases.values()) {
                Type other = s.fAliases.get(alias.getName());
                if (other != null && !other.comparable(alias)) {
                    throw new FactTypeRedeclaredException(alias.getName(), other);
                }
            }
        }
    }

    /**
    * @return the singleton instance of the {@link TypeFactory}
    */
    public TypeFactory getFactory() {
        return factory;
    }

    /**
    * Declare an alias type. The alias may be parameterized to make an abstract alias.
    * Each ParameterType embedded in the aliased type should occur in the list of parameters.
    *
    * @param alias the alias to declare in this store
    * @throws FactTypeRedeclaredException
    */
    public void declareAlias(Type alias) throws FactTypeDeclarationException {
        synchronized (fADTs) {
            synchronized (fAliases) {
                String name = alias.getName();
                Type oldAdt = lookupAbstractDataType(name);
                if (oldAdt != null) {
                    throw new FactTypeRedeclaredException(name, oldAdt);
                }

                Type oldAlias = lookupAlias(name);

                if (oldAlias != null) {
                    if (oldAlias == alias || (!alias.isOpen() && alias.isSubtypeOf(oldAlias))) {
                        // instantiating can be ignored.
                        return;
                    }
                    throw new FactTypeRedeclaredException(name, oldAlias);
                }

                fAliases.put(name, alias);
            }
        }
    }

    /**
    * Declare a @{link AbstractDataType}, which is a kind of tree node. Each kind of tree node
    * may have different alternatives, which are ConstructorTypes. A @{link ConstructorType} is always a
    * sub-type of a AbstractDataType. A AbstractDataType is always a sub type of value.
    *
    * @param adt the abstract data-type to declare in this store
    * @throws FactTypeRedeclaredException
    */
    public void declareAbstractDataType(Type adt)
        throws FactTypeDeclarationException {
        synchronized (fADTs) {
            synchronized (fAliases) {
                synchronized (fConstructors) {
                    String name = adt.getName();
                    Type oldAdt = lookupAbstractDataType(name);

                    if (oldAdt != null) {
                        if (adt.comparable(oldAdt) || oldAdt.isExternalType()) {
                            return; // paramaterized ADT got instantiated, or a double declaration, so don't store.
                        }

                        throw new FactTypeRedeclaredException(name, oldAdt);
                    }

                    Type oldAlias = lookupAlias(name);
                    if (oldAlias != null) {
                        throw new FactTypeRedeclaredException(name, oldAlias);
                    }

                    fADTs.put(name, adt);

                    if (fConstructors.get(adt) == null) {
                        fConstructors.put(adt, new HashSet<Type>());
                    }
                }
            }
        }
    }

    /**
    * Declare a new constructor type. A constructor type extends an abstract data type such
    * that it represents more values.
    *
    * @param constructor a constructor type
    * @throws UndeclaredAbstractDataTypeException, RedeclaredFieldNameException, RedeclaredConstructorException
    */
    public void declareConstructor(Type constructor) throws FactTypeDeclarationException {
        synchronized (fADTs) {
            synchronized(fConstructors) {
                Type adt = constructor.getAbstractDataType();

                Type other = lookupAbstractDataType(adt.getName());
                if (other == null) {
                    throw new UndeclaredAbstractDataTypeException(adt);
                }

                Set<Type> signature = lookupAlternatives(adt);
                if (signature == null) {
                    throw new UndeclaredAbstractDataTypeException(adt);
                }

                Type constructor1 = expandAliases(constructor);
                if(!constructor.equals(constructor1)){
                    constructor = constructor1;
                }
                if(!IGNORE_OVERLOADING_CHECKS.contains(adt.getName())){
                    checkOverloading(signature, constructor.getName(), constructor.getFieldTypes());
                    try {
                        checkFieldNames(signature, constructor.getFieldTypes());
                    }
                    catch (RedeclaredFieldNameException e) {
                        throw new RedeclaredFieldNameException(e.getFieldName(),
                        e.getFirstType(), e.getSecondType(),
                        adt);
                    }
                }

                Set<Type> localSignature = fConstructors.get(adt);
                if (localSignature == null) {
                    localSignature = new HashSet<>();
                    fConstructors.put(adt, localSignature);

                    if (!fADTs.containsKey(adt.getName())) {
                        fADTs.put(adt.getName(), adt);
                    }
                }

                localSignature.add(constructor);
            }
        }
    }

    private void checkFieldNames(Set<Type> signature, Type tupleType) {
        if (!tupleType.hasFieldNames()) {
            return;
        }

        for (Type alt : signature) {
            Type altArgs = alt.getFieldTypes();
            if (!altArgs.hasFieldNames()) {
                continue;
            }
            for (int i = tupleType.getArity() - 1; i >= 0; i--) {
                Type type = tupleType.getFieldType(i);
                String label = Objects.requireNonNull(tupleType.getFieldName(i));

                for (int j = altArgs.getArity() - 1; j >= 0; j--) {
                    if (label.equals(altArgs.getFieldName(j))) {
                        if (!altArgs.getFieldType(j).equivalent(type)) {
                            throw new RedeclaredFieldNameException(label, type, altArgs.getFieldType(j), tupleType);
                        }
                    }
                }
            }
        }
    }

    private void checkOverloading(Set<Type> signature, String name, Type tupleType) throws FactTypeDeclarationException {
        for (Type alt : signature) {
            if (alt.getName().equals(name)) {
                Type fieldTypes = alt.getFieldTypes();
                if (fieldTypes != tupleType && fieldTypes.comparable(tupleType)) {
                    throw new RedeclaredConstructorException(name, fieldTypes, tupleType);
                }
            }
        }
    }

    /**
    * Lookup a AliasType that was declared before by name
    * @param name the name of the type to lookup
    * @return the AliasType
    */
    public @Nullable Type lookupAlias(final String name) {
        synchronized (fAliases) {
            synchronized (fImports) {
                Type result = fAliases.get(name);

                if (result == null) {
                    for (TypeStore i : fImports) {
                        result = i.fAliases.get(name);
                        if (result != null) {
                            return result;
                        }
                    }
                }

                return result;
            }
        }
    }

    /**
    * Returns all alternative ways of constructing a certain abstract data type.
    *
    * @param adt
    * @return all types that construct the given type
    */
    public Set<Type> lookupAlternatives(Type adt) {
        synchronized (fConstructors) {
            synchronized (fImports) {
                while (adt.isAliased()) {
                    adt = adt.getAliased();
                }

                Set<Type> result = fConstructors.get(adt);

                if (result == null) {
                    result = new HashSet<>();
                }

                for (TypeStore s : fImports) {
                    if (s != this) {
                        Set<Type> imported = s.fConstructors.get(adt);
                        if (imported != null) {
                            result.addAll(imported);
                        }
                    }
                }

                return result;
            }
        }
    }

    /**
    * Lookup a ConstructorType by name, and in the context of a certain AbstractDataType
    * @param adt             the AbstractDataType context
    * @param constructorName  the name of the ConstructorType
    * @return a ConstructorType if it was declared before
    * @throws a FactTypeError if the type was not declared before
    */
    public Set<Type> lookupConstructor(Type adt, String constructorName) throws FactTypeUseException {
        synchronized (fConstructors) {
            synchronized (fImports) {
                while (adt.isAliased()) {
                    adt = adt.getAliased();
                }
                Type parameterizedADT = fADTs.get(adt.getName());
                Set<Type> local = parameterizedADT != null ? fConstructors.get(parameterizedADT) : null;
                Set<Type> result = new HashSet<>();

                if (local != null) {
                    for (Type cand : local) {
                        if (cand.getName().equals(constructorName)) {
                            result.add(cand);
                        }
                    }
                }

                for (TypeStore i : fImports) {
                    local = i.fConstructors.get(adt);
                    if (local != null) {
                        for (Type cand : local) {
                            if (cand.getName().equals(constructorName)) {
                                result.add(cand);
                            }
                        }
                    }
                }

                return result;
            }
        }
    }

    /**
    * Lookup a ConstructorType by name, across all AbstractDataTypes and for
    * a certain list of argument types.
    *
    * @param constructorName  the name of the ConstructorType
    * @param args a tuple type defining the arguments of the constructor
    * @return the first constructor that matches
    * @throws a FactTypeError if the type was not declared before
    */
    public @Nullable Type lookupFirstConstructor(final String cons, final Type args) {
        Collection<Type> adts = allAbstractDataTypes();

        for (Type adt : adts) {
            Type cand = lookupConstructor(adt, cons, args);
            if (cand != null) {
                return cand;
            }
        }

        return null;
    }

    private Set<Type> allAbstractDataTypes() {
        synchronized (fADTs) {
            synchronized (fImports) {
                Set<Type> result = new HashSet<>();
                result.addAll(fADTs.values());

                for (TypeStore s : fImports) {
                    result.addAll(s.fADTs.values());
                }

                return result;
            }
        }
    }

    /**
    * Lookup a ConstructorType by name, and in the context of a certain AbstractDataType
    * for a specific list of argument types.
    *
    * @param adt             the AbstractDataType context
    * @param constructorName  the name of the ConstructorType
    * @return a ConstructorType if it was declared before
    * @throws a FactTypeError if the type was not declared before
    */
    public @Nullable Type lookupConstructor(Type adt, String cons, Type args) {
        Set<Type> sig = lookupConstructor(adt, cons);

        if (sig != null) {
            for (Type cand : sig) {
                if (args.isSubtypeOf(cand.getFieldTypes())) {
                    return cand;
                }
            }
        }

        return null;
    }

    /**
    * Retrieve all tree node types for a given constructor name,
    * regardless of abstract data-type.
    *
    * @param constructName the name of the tree node
    */
    public Set<Type> lookupConstructors(String constructorName) {
        synchronized (fConstructors) {
            synchronized (fImports) {
                Set<Type> result = new HashSet<>();

                for (Set<Type> adt : fConstructors.values()) {
                    for (Type cand : adt) {
                        String name = cand.getName();
                        if (name.equals(constructorName)) {
                            result.add(cand);
                        }
                    }
                }

                for (TypeStore i : fImports) {
                    if (i != this) {
                        for (Set<Type> adt : i.fConstructors.values()) {
                            for (Type cand : adt) {
                                String name = cand.getName();
                                if (name.equals(constructorName)) {
                                    result.add(cand);
                                }
                            }
                        }
                    }
                }

                return result;
            }
        }
    }

    /**
    * See if a certain abstract data-type was declared
    * @param name  the supposed name of the abstract data-type
    * @return null if such type does not exist, or the type if it was declared earlier
    */
    public @Nullable Type lookupAbstractDataType(String name) {
        synchronized (fADTs) {
            synchronized (fImports) {

                Type result = fADTs.get(name);

                if (result != null) {
                    return result;
                }

                for (TypeStore s : fImports) {
                    result = s.fADTs.get(name);
                    if (result != null) {
                        return result;
                    }
                }

                return result;
            }
        }
    }

    /**
    * Declare that certain  constructor types may have an keyword parameter with a certain
    * label. The keyword parameter with that label will have a specific type. Note that we
    * do not store keyword parameters directly inside the constructor type because keyword
    * parameter can be added externally and constructor types are final (like all other types).
    *
    * @param onType the constructor type of values that carry this keyword parameter
    * @param key    the label of the keyword parameter
    * @param valueType the type of values that represent the keyword parameter
    * @throws IllegalKeywordParameterDeclarationException when an attempt is made to define keyword parameter for anything
    * but constructor types
    */
    public void declareKeywordParameter(Type onType, String key, Type valueType) {
        if (!onType.isConstructor() && !onType.isAbstractData()) {
            throw new IllegalKeywordParameterDeclarationException(onType);
        }

        onType = expandAliases(onType);

        synchronized (fkeywordParameters) {
            Map<String, Type> kwParamsForType = fkeywordParameters.get(onType);

            if (!factory.isIdentifier(key)) {
                throw new IllegalIdentifierException(key);
            }

            if (kwParamsForType == null) {
                kwParamsForType = new HashMap<>();
                fkeywordParameters.put(onType, kwParamsForType);
            }

            Map<String, Type> declaredEarlier = getKeywordParameters(onType);

            if (!declaredEarlier.containsKey(key)) {
                kwParamsForType.put(key, valueType);
            }
            else if (!declaredEarlier.get(key).equivalent(valueType)) {
                throw new RedeclaredKeywordParameterException(key, declaredEarlier.get(key));
            }
            // otherwise its a safe re-declaration and we do nothing
        }
    }

    // TODO: aliases are right now only expanded in declareConstructor, but this should also be done
    // in at least declareAlias, declareAnnotation and declareKeywordParameters.

    @Deprecated
    /**
    * Aliases are deprecated. When we remove this, everything becomes faster.
    * @return
    */
    private Type expandAliases(Type type) {
        return expandAliases1(type, new HashSet<String>());
    }

    private Type expandAliases1 (Type type, Set<String> seen){
        return type.accept(new ITypeVisitor<Type,RuntimeException>() {

            @Override
            public Type visitReal(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitInteger(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitRational(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitList(Type type) throws RuntimeException {
                return factory.listType(expandAliases1(type.getElementType(), seen));
            }

            @Override
            public Type visitMap(Type type) throws RuntimeException {
                return factory.mapType(expandAliases1(type.getKeyType(), seen), expandAliases1(type.getValueType(), seen));
            }

            @Override
            public Type visitNumber(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitAlias(Type type) throws RuntimeException {
                String aliasName = type.getName();
                if(seen.contains(aliasName)){
                    throw new IllegalIdentifierException("Circular alias definition for: " + aliasName);
                }
                seen.add(aliasName);
                return expandAliases1 (type.getAliased(), seen);
            }

            @Override
            public Type visitSet(Type type) throws RuntimeException {
                return factory.setType(expandAliases1 (type.getElementType(), seen));
            }

            @Override
            public Type visitSourceLocation(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitString(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitNode(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitConstructor(Type type) throws RuntimeException {
                // TODO: this cache is nasty coupling with TypeFactory, but we
                // can not call factory.constructor directly because that would produce
                // an infinite recursion.
                return factory.getFromCache(new ConstructorType(type.getName(), expandAliases1(type.getFieldTypes(), seen), type.getAbstractDataType()));
            }

            @Override
            public Type visitAbstractData(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitFunction(Type type) throws RuntimeException {
                // this call to getFromCache is nasty coupling with TypeFactory, but we
                // can not call factory.constructor directly because that would produce
                // an infinite recursion.
                return factory.getFromCache(new FunctionType(expandAliases1(type.getReturnType(), seen), (TupleType) expandAliases1(type.getFieldTypes(), seen), (TupleType) expandAliases1(type.getKeywordParameterTypes(), seen)));
            }

            @SuppressWarnings("deprecation")
            @Override
            public Type visitTuple(Type type) throws RuntimeException {
                int arity = type.getArity();
                Type fieldTypes[] = new Type[arity];
                boolean aliasFound = false;
                for(int i = 0; i < arity; i++){
                    Type fieldType = type.getFieldType(i);

                    if(fieldType.isAliased()){
                        aliasFound = true;
                        AliasType alias = (AliasType) fieldType;
                        fieldType = alias.getAliased();
                    }

                    Type newFieldType = expandAliases1(fieldType, seen);
                    aliasFound |= newFieldType != fieldType;
                    fieldTypes[i] = newFieldType;
                }

                if (aliasFound){
                    return type.hasFieldNames() ? factory.tupleType(fieldTypes, type.getFieldNames()) : factory.tupleType(fieldTypes);
                }
                return type;
            }

            @Override
            public Type visitValue(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitVoid(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitBool(Type type) throws RuntimeException {
                return type;
            }

            @Override
            public Type visitParameter(Type type) throws RuntimeException {
                return factory.parameterType(type.getName(), expandAliases1(type.getBound(), seen));
            }

            @Override
            public Type visitExternal(Type type) throws RuntimeException {
                // TODO: it is unclear how aliases in externalTypes can be expanded in an extensible fashion
                // (rather than listing all cases here).
                return type;
            }

            @Override
            public Type visitDateTime(Type type) throws RuntimeException {
                return type;
            }

        });
    }

    /**
    * Locates all declared keyword parameters for a constructor.
    *
    * @param onType
    * @return a map of all keyword parameters declared for the onType constructor
    */
    public Map<String, Type> getKeywordParameters(Type onType) {
        if (!onType.isConstructor() && !onType.isAbstractData()) {
            return Collections.<String,Type>emptyMap();
        }

        onType = expandAliases(onType);

        synchronized(fkeywordParameters) {
            synchronized (fImports) {
                Map<String, Type> result = new HashMap<>();

                Map<String, Type> local = fkeywordParameters.get(onType);
                if (local != null) {
                    result.putAll(local);
                }

                if (onType.isConstructor()) {
                    local = fkeywordParameters.get(onType.getAbstractDataType());
                    if (local != null) {
                        result.putAll(local);
                    }
                }

                for (TypeStore s : fImports) {
                    if (s.fkeywordParameters == null) {
                        continue;
                    }
                    Map<String, Type> here = s.fkeywordParameters.get(onType);
                    if (here != null) {
                        result.putAll(here);
                    }

                    if (onType.isConstructor()) {
                        here = s.fkeywordParameters.get(onType.getAbstractDataType());
                        if (here != null) {
                            result.putAll(here);
                        }
                    }
                }

                return result;
            }
        }
    }

    /**
    * Retrieve the type of values that are declared to be valid for a certain kind of
    * keyword parameters on certain kinds of values
    * @param onType the constructor type that this keyword parameters can be found on
    * @param key    the label of the parameter to find the corresponding type of
    * @return the type of the requested parameter value or null if none exists
    */
    public @Nullable Type getKeywordParameterType(Type onType, String key) {
        assert onType.isConstructor() || onType.isAbstractData();
        Map<String, Type> kwParamsFor = getKeywordParameters(onType);
        return kwParamsFor != null ? kwParamsFor.get(key) : null;
    }

    public boolean hasKeywordParameters(Type onType) {
        if (!onType.isConstructor()) {
            return false;
        }

        synchronized(fkeywordParameters) {
            synchronized (fImports) {
                Map<String, Type> local = fkeywordParameters.get(onType);
                if (local != null && local.size() > 0) {
                    return true;
                }

                for (TypeStore s : fImports) {
                    if (s.fkeywordParameters == null) {
                        continue;
                    }

                    Map<String, Type> here = s.fkeywordParameters.get(onType);
                    if (here != null && here.size() > 0) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public boolean hasKeywordParameter(Type onType, String label) {
        if (!onType.isConstructor()) {
            return false;
        }

        onType = expandAliases(onType);

        synchronized(fkeywordParameters) {
            synchronized (fImports) {
                Map<String, Type> local = fkeywordParameters.get(onType);
                if (local != null && local.containsKey(label)) {
                    return true;
                }

                local = fkeywordParameters.get(onType.getAbstractDataType());
                if (local != null && local.containsKey(label)) {
                    return true;
                }

                for (TypeStore s : fImports) {
                    if (s.fkeywordParameters == null) {
                        continue;
                    }

                    Map<String, Type> here = s.fkeywordParameters.get(onType);
                    if (here != null && here.containsKey(label)) {
                        return true;
                    }

                    here = s.fkeywordParameters.get(onType.getAbstractDataType());
                    if (here != null && here.containsKey(label)) {
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public @Nullable Type getAlias(String name) {
        synchronized (fAliases) {
            synchronized (fImports) {
                Type result = fAliases.get(name);

                if (result != null) {
                    return result;
                }

                for (TypeStore s : fImports) {
                    result = s.fAliases.get(name);
                    if (result != null) {
                        return result;
                    }
                }

                return null;
            }
        }
    }
}
