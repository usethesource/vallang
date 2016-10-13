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

package org.rascalmpl.value.type;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.IList;
import org.rascalmpl.value.ISetWriter;
import org.rascalmpl.value.IString;
import org.rascalmpl.value.IValue;
import org.rascalmpl.value.IValueFactory;
import org.rascalmpl.value.exceptions.FactTypeDeclarationException;
import org.rascalmpl.value.exceptions.IllegalFieldNameException;
import org.rascalmpl.value.exceptions.IllegalFieldTypeException;
import org.rascalmpl.value.exceptions.IllegalIdentifierException;
import org.rascalmpl.value.exceptions.NullTypeException;

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
	private final Map<Type, Type> fCache = new HashMap<Type, Type>();
	
	private static class InstanceHolder {
		public static final TypeFactory sInstance = new TypeFactory();
		public static final TypeValues typeSymbolInstance = sInstance.new TypeValues();
	}

	private TypeFactory() { }

	public static TypeFactory getInstance() {
		return InstanceHolder.sInstance;
	}
	
	public TypeValues getSymbols() {
		return InstanceHolder.typeSymbolInstance;
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
	 * Reconstruct a type from its symbolic value representation, and if relevant add
	 * its declaration to the store (for ADTs, constructors and aliases)
	 * 
	 * @param symbol  value representation of a type
	 * @param store   store to put declarations in
	 * @param grammar function to look up definitions for non-terminal types
	 * @return a type isomorphic to the given symbolic representation
	 */
	public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar) {
		return getSymbols().fromSymbol(symbol, store, grammar);
	}
	
	/**
	   * Represent this type as a value of the abstract data-type "Symbol". As a side-effect
	   * it will also add Production values to the grammar map, including all necessary productions
	   * to build values of the receiver type, transitively.
	   * 
	   * @param  vf valuefactory to use 
	   * @param store store to lookup additional necessary definitions in to store in the grammar
	   * @param grammar map to store production values in as a side-effect
	   * @param done a working set to store data-types which have been explored already to avoid infinite recursion
	   * @return a value to uniquely represent this type.
	   */
	public IConstructor asSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
		return type.asSymbol(vf, store, grammar, done);
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

	private void checkNull(Object... os) {
		for (int i = os.length - 1; i >= 0; i--) {
			if (os[i] == null)
				throw new NullTypeException();
		}
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

	public static interface TypeReifier {
		default TypeValues symbols() {
			return tf().getSymbols();
		}
		
		default TypeFactory tf() {
			return TypeFactory.getInstance();
		}
		
		Type getSymbolConstructorType();
		
		default IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
			// this will work for all nullary type symbols
			return vf.constructor(getSymbolConstructorType());
		}
		
		default void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
			// normally nothing
		}
		
		Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar);
	}

	public class TypeValues {
		private static final String TYPES_CONFIG = "org/rascalmpl/value/types.config";
		
		private final TypeStore symbolStore = new TypeStore();
		private final Type Symbol = abstractDataType(symbolStore, "Symbol");
		private final Type Symbol_Label = constructor(symbolStore, Symbol, "label", stringType(), "name", Symbol, "symbol");
	
		private final Map<Type, TypeReifier> symbolConstructorTypes = new HashMap<>();
		
		private TypeValues() { loadReifiers(); }
		
		public boolean isLabel(IConstructor symbol) {
			return symbol.getConstructorType() == Symbol_Label;
		}
	
		public String getLabel(IValue symbol) {
			return ((IString) ((IConstructor) symbol).get("name")).getValue();
		}
		
		public IConstructor labelSymbol(IValueFactory vf, IConstructor symbol, String label) {
			return vf.constructor(Symbol_Label, vf.string(label), symbol);
		}
	
		public IConstructor getLabeledSymbol(IValue symbol) {
			return (IConstructor) ((IConstructor) symbol).get("symbol");
		}
		
		protected Type typeSymbolConstructor(String name, Object... args) {
			return constructor(symbolStore, symbolADT(), name, args);
		}
	
		protected Type typeProductionConstructor(String name, Object... args) {
			return constructor(symbolStore, productionADT(), name, args);
		}
	
		protected Type symbolADT() {
			return abstractDataType(symbolStore, "Symbol");
		}
	
		protected Type productionADT() {
			return abstractDataType(symbolStore, "Production");
		}
		
		private void loadReifiers() {
			try {
				Enumeration<URL> resources = getClass().getClassLoader().getResources(TYPES_CONFIG);
				while (resources.hasMoreElements()) {
					loadServices(resources.nextElement());
				}
			} catch (IOException e) {
				throw new IllegalArgumentException("WARNING: Could not load type info extensions from " + TYPES_CONFIG);
			}
		}
		
		private void loadServices(URL nextElement) throws IOException {
		    for (String name : readConfigFile(nextElement)) {
		        name = name.trim();
	
		        if (name.startsWith("#")) { 
		            // source code comment
		            continue;
		        }
	
		        try {
		            Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(name);
		            Object instance = clazz.newInstance();
	
		            if (instance instanceof TypeReifier) {
		            	registerTypeInfo((TypeReifier) instance);
		            }
		            else {
		                throw new IllegalArgumentException("WARNING: could not load type info " + name + " because it does not implement TypeFactory.TypeInfo");
		            }
		        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException | IllegalArgumentException | SecurityException e) {
		            throw new IllegalArgumentException("WARNING: could not load type info " + name + " due to " + e.getMessage());
		        }
		    }
		}
	
	    private void registerTypeInfo(TypeReifier instance) {
			symbolConstructorTypes.put(instance.getSymbolConstructorType(), instance);
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
			return symbolConstructorTypes.get(symbol.getConstructorType()).fromSymbol(symbol, store, grammar);
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
		return getSymbols().fromSymbol(symbol, new TypeStore(), x -> Collections.emptySet());
	}

}
