/*******************************************************************************
 * Copyright (c) 2007, 2008, 2012 IBM Corporation and CWI
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

package org.eclipse.imp.pdb.facts.type;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeDeclarationException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalFieldNameException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalFieldTypeException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalIdentifierException;
import org.eclipse.imp.pdb.facts.exceptions.NullTypeException;

/**
 * Use this class to produce any kind of {@link Type}, after which the make
 * methods of Type can be used in conjunction with a reference to an
 * {@link IValueFactory} to produce {@link IValue}'s of a certain type.
 * <p>
 * 
 * @see {@link Type} and {@link IValueFactory} for more information.
 */
public class TypeFactory {
  private static class InstanceHolder {
    public static final TypeFactory sInstance = new TypeFactory();
  }

  /**
   * Caches all types to implement canonicalization
   */
  private final Map<Type, Type> fCache = new HashMap<Type, Type>();

  public static TypeFactory getInstance() {
    return InstanceHolder.sInstance;
  }

  private TypeFactory() {
    super();
  }

  private void checkNull(Object... os) {
    for (int i = os.length - 1; i >= 0; i--) {
      if (os[i] == null)
        throw new NullTypeException();
    }
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

  private Type getFromCache(Type t) {
    synchronized (fCache) {
      Type result = fCache.get(t);

      if (result == null) {
        fCache.put(t, t);
        return t;
      }

      return result;
    }
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
    checkNull(externalType);
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
   * @return a tuple type
   */
  public Type tupleType(Type... fieldTypes) {
    checkNull((Object[]) fieldTypes);
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
      } catch (ClassCastException e) {
        throw new IllegalFieldTypeException(pos, fieldTypesAndLabels[i], e);
      }
      try {
        String name = (String) fieldTypesAndLabels[i + 1];
        if (!isIdentifier(name))
          throw new IllegalIdentifierException(name);
        protoFieldNames[pos] = name;
      } catch (ClassCastException e) {
        throw new IllegalFieldNameException(pos, fieldTypesAndLabels[i + 1], e);
      }
    }

    return getFromCache(new TupleType(protoFieldTypes, protoFieldNames));
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
    checkNull((Object[]) types);
    checkNull((Object[]) labels);
    return getFromCache(new TupleType(types, labels));
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
    checkNull((Object[]) elements);
    int N = elements.length;
    Type[] fieldTypes = new Type[N];
    for (int i = N - 1; i >= 0; i--) {
      fieldTypes[i] = elements[i].getType();
    }
    return getOrCreateTuple(fieldTypes);
  }

  /**
   * Construct a set type
   * 
   * @param eltType
   *          the type of elements in the set
   * @return a set type
   */
  public Type setType(Type eltType) {
    checkNull(eltType);
    return getFromCache(new SetType(eltType));
  }

  public Type relTypeFromTuple(Type tupleType) {
    checkNull(tupleType);
    return setType(tupleType);
  }

  public Type lrelTypeFromTuple(Type tupleType) {
    checkNull(tupleType);
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
    checkNull((Object[]) fieldTypes);
    return setType(tupleType(fieldTypes));
  }

  /**
   * Construct a relation type.
   * 
   * @param fieldTypes
   *          the types of the fields of the relation
   * @return a relation type
   */
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
    checkNull((Object[]) fieldTypes);
    return listType(tupleType(fieldTypes));
  }

  /**
   * Construct a list relation type.
   * 
   * @param fieldTypes
   *          the types of the fields of the relation
   * @return a list relation type
   */
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
    checkNull(store, name, aliased);
    checkNull((Object[]) parameters);

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
    checkNull(store);
    checkNull(name);
    checkNull(aliased);
    checkNull(params);

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
    checkNull(store, name);
    checkNull((Object[]) parameters);

    Type paramType = voidType();
    if (parameters.length != 0) {
      paramType = tupleType(parameters);
    }

    return abstractDataTypeFromTuple(store, name, paramType);
  }

  public Type abstractDataTypeFromTuple(TypeStore store, String name, Type params) throws FactTypeDeclarationException {
    checkNull(store, name, params);

    if (!isIdentifier(name)) {
      throw new IllegalIdentifierException(name);
    }
    Type result = getFromCache(new AbstractDataType(name, params));

    if (!params.equivalent(voidType())) {
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
  public Type constructorFromTuple(TypeStore store, Type adt, String name, Type tupleType)
      throws FactTypeDeclarationException {
    checkNull(store, adt, name, tupleType);

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
	 * Special case of @see TypeFactory#constructor(TypeStore, Type, String,
	 * Type...) with without varargs. It is necessary because the Eclipse Luna
	 * compiler reports an ambiguity onder Java 8 (whereas Oracle's compiler
	 * does not).
	 */
  @Deprecated
  public Type constructor(TypeStore store, Type adt, String name) throws FactTypeDeclarationException {
    return constructorFromTuple(store, adt, name, tupleEmpty());
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
    checkNull(elementType);
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
    checkNull(key, value);
    return getFromCache(new MapType(key, value));
  }

  public Type mapTypeFromTuple(Type fields) {
    checkNull(fields);
    if (!fields.isFixedWidth()) {
      throw new UnsupportedOperationException("fields argument should be a tuple. not " + fields);
    }
    if (fields.getArity() < 2) {
      throw new IndexOutOfBoundsException();
    }
    if (fields.hasFieldNames()) {
      return mapType(fields.getFieldType(0), fields.getFieldName(0), fields.getFieldType(1), fields.getFieldName(1));
    } else {
      return mapType(fields.getFieldType(0), fields.getFieldType(1));
    }
  }

  public Type mapType(Type key, String keyLabel, Type value, String valueLabel) {
    checkNull(key, value);
    if ((keyLabel != null && valueLabel == null) || (valueLabel != null && keyLabel == null)) {
      throw new IllegalArgumentException("Key and value labels must both be non-null or null: " + keyLabel + ", "
          + valueLabel);
    }
    return getFromCache(new MapType(key, keyLabel, value, valueLabel));
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
    checkNull(name, bound);
    return getFromCache(new ParameterType(name, bound));
  }

  /**
   * Construct a type parameter, which can later be instantiated.
   * 
   * @param name
   *          the name of the type parameter
   * @return a parameter type
   */
  public Type parameterType(String name) {
    checkNull(name);
    return getFromCache(new ParameterType(name));
  }

  /**
   * Checks to see if a string is a valid PDB type, field or annotation
   * identifier
   * 
   * @param str
   * @return true if the string is a valid identifier
   */
  public boolean isIdentifier(String str) {
    checkNull(str);

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

}
