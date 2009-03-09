/*******************************************************************************
* Copyright (c) 2007, 2008 IBM Corporation and CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) 
*    Jurgen Vinju  (jurgen@vinju.org)       
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
 * Use this class to produce any kind of {@link Type}, after which
 * the make methods of Type can be used in conjunction with a reference
 * to an {@link IValueFactory} to produce {@link IValue}'s of a certain
 * type. 
 * <p>
 * @see {@link Type} and {@link IValueFactory} for more information.
 */
public class TypeFactory {
	private static class InstanceHolder {
      public static final TypeFactory sInstance = new TypeFactory();
	}

    /**
     * Caches all types to implement canonicalization
     */
	private final Map<Type,Type> fCache = new HashMap<Type,Type>();


    public static TypeFactory getInstance() {
        return InstanceHolder.sInstance;
    }

    private TypeFactory() { }
    
    private void checkNull(Object ... os) {
    	for (Object o : os) {
    		if (o == null) {
    			throw new NullTypeException();
    		}
    	}
    }

    /**
     * construct the value type, which is the top type of the type hierarchy
     * representing all possible values.
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

    private Type getFromCache(final Type t) {
    	synchronized(fCache){
			final Type result = fCache.get(t);
	
			if (result == null) {
				fCache.put(t, t);
				return t;
			}
			else {
			  return result;
			}
    	}
    }
    
    /**
     * Construct a new type. 
     * @return a reference to the unique integer type of the PDB.
     */
    public Type integerType() {
        return IntegerType.getInstance();
    }

    /**
     * Construct a new type. 
     * @return a reference to the unique double type of the PDB.
     */
    public Type realType() {
        return RealType.getInstance();
    }

    /**
     * Construct a new bool type
     * @return a reference to the unique boolean type of the PDB.
     */
    public Type boolType() {
		return BoolType.getInstance();
	}
    
    /**
     * Construct a new type. 
     * @return a reference to the unique string type of the PDB.
     */
    public Type stringType() {
        return StringType.getInstance();
    }

    /**
     * Construct a new type. 
     * @return a reference to the unique sourceLocation type of the PDB.
     */
    public Type sourceLocationType() {
        return SourceLocationType.getInstance();
    }

    private TupleType getOrCreateTuple(int size, Type[] fieldTypes) {
    	return (TupleType) getFromCache(new TupleType(size, 0, fieldTypes));
    }
    
    /**
     * Construct a tuple type. 
     * @return a reference to the unique empty tuple type.
     */
    public Type tupleEmpty() {
    	return (TupleType) getFromCache(new TupleType(0, 0, new Type[0]));
    }
    
    /**
     * Construct a tuple type.
     * @param fieldTypes a list of field types in order of appearance.
     * @return a tuple type
     */
    public Type tupleType(Type... fieldTypes) {
    	checkNull((Object[]) fieldTypes);
    	return (TupleType) getFromCache(new TupleType(fieldTypes.length, 0, fieldTypes));
    }

    /**
     * Construct a labeled tuple type
     * 
     * @param fieldTypesAndLabel an array of field types, where each field type of type @{link Type}
     *        is immediately followed by a field label of type @{link String}.
     * @return a tuple type
     * @throws FactTypeDeclarationException when one of the labels is not a proper identifier or when
     *         the argument array does not contain alternating types and labels.
     */
    public Type tupleType(Object... fieldTypesAndLabels) throws FactTypeDeclarationException {
    	int N= fieldTypesAndLabels.length;
        int arity = N / 2;
		Type[] protoFieldTypes= new Type[arity];
        String[] protoFieldNames = new String[arity];
        for(int i=0; i < N; i+=2) {
            int pos = i / 2;
            if (fieldTypesAndLabels[i] == null || fieldTypesAndLabels[i+1] == null) {
            	throw new NullPointerException();
            }
            try {
            	protoFieldTypes[pos]= (Type) fieldTypesAndLabels[i];
            } 
            catch (ClassCastException e) {
            	throw new IllegalFieldTypeException(pos, fieldTypesAndLabels[i], e);
            }
            try {
            	protoFieldNames[pos] = (String) fieldTypesAndLabels[i+1];
            }
            catch (ClassCastException e) {
            	throw new IllegalFieldNameException(pos, fieldTypesAndLabels[i+1], e);
            }
        }
        
        for (String name : protoFieldNames) {
        	if (!isIdentifier(name)) {
        		throw new IllegalIdentifierException(name);
        	}
        }
        return (TupleType) getFromCache(new TupleType(arity, 0, protoFieldTypes, protoFieldNames));
    }
    
	/**
     * Construct a tuple type. The length of the types and labels arrays should be equal.
     * 
     * @param types  the types of the fields
     * @param labels the labels of the fields (in respective order)
     * @return
     */
    public Type tupleType(Type[] types, String[] labels) {
    	checkNull((Object[]) types);
    	checkNull((Object[]) labels);
    	return (TupleType) getFromCache(new TupleType(types.length, 0, types, labels));
    }
    
    /**
     * Construct a tuple type.
     * @param fieldTypes an array of field types in order of appearance. The array is copied.
     * @return a tuple type
     */
    public Type tupleType(IValue... elements) {
    	checkNull((Object[]) elements);
        int N= elements.length;
        Type[] fieldTypes= new Type[N];
        for(int i=0; i < N; i++) {
            fieldTypes[i]= elements[i].getType();
        }
        return getOrCreateTuple(N, fieldTypes);
    }
    
    /**
     * Construct a set type
     * @param eltType the type of elements in the set
     * @return a set type
     */
    public Type setType(Type eltType) {
    	checkNull(eltType);
    	if (eltType.isTupleType()) {
    		return relTypeFromTuple(eltType);
    	}
    	else {
          return (SetType) getFromCache(new SetType(eltType));
    	}
    }

    public Type relTypeFromTuple(Type tupleType) {
    	checkNull(tupleType);
    	return getFromCache(new RelationType(tupleType));
    }
    
    /**
     * Construct a relation type.
     * @param fieldTypes the types of the fields of the relation
     * @return a relation type
     */
    public Type relType(Type... fieldTypes) {
    	checkNull((Object[]) fieldTypes);
        return getFromCache(new RelationType(tupleType(fieldTypes)));
    }
    
    /**
     * Construct a relation type.
     * @param fieldTypes the types of the fields of the relation
     * @return a relation type
     */
    public Type relType(Object... fieldTypesAndLabels) {
        return relTypeFromTuple(tupleType(fieldTypesAndLabels));
    }

    /** 
     * Construct an alias type. The alias may be parameterized to make an abstract alias. 
     * Each ParameterType embedded in the aliased type should occur in the list of parameters.
     * 
     * @param store      to store the declared alias in
     * @param name       the name of the type
     * @param aliased    the type it should be an alias for
     * @param parameters a list of type parameters for this alias
     * @return an alias type
     * @throws FactTypeDeclarationException if a type with the same name but a different supertype was defined earlier as a named type of a AbstractDataType.
     */
    public Type aliasType(TypeStore store, String name, Type aliased, Type...parameters) throws FactTypeDeclarationException {
    	checkNull(store, name, aliased);
    	checkNull((Object[]) parameters);
    	
    	Type paramType;
    	if (parameters.length == 0) {
    		paramType = voidType();
    	}
    	else {
    		paramType = tupleType(parameters);
    	}

    	return aliasTypeFromTuple(store, name, aliased, paramType);
    }
    
    public Type aliasTypeFromTuple(TypeStore store, String name, Type aliased, Type params) throws FactTypeDeclarationException {
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
     * Construct a @{link AbstractDataType}, which is a kind of tree node. Each kind of tree node
     * may have different alternatives, which are ConstructorTypes. A @{link ConstructorType} is always a
     * sub-type of a AbstractDataType. A AbstractDataType is always a sub type of value.
     * 
     * @param store to store the declared adt in
     * @param name the name of the abstract data-type
     * @param parameters array of type parameters
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
    	store.declareAbstractDataType(result);
    	return result;
    }
    
    /**
    * Make a new constructor type. A constructor type extends an abstract data type such
     * that it represents more values.
     * 
     * @param store to store the declared constructor in
     * @param adt the AbstractDataType this constructor builds
     * @param name     the name of the node type
     * @param children the types of the children of the tree node type
     * @return a tree node type
     * @throws IllegalIdentifierException, UndeclaredAbstractDataTypeException, RedeclaredFieldNameException, RedeclaredConstructorException
     */
    public Type constructorFromTuple(TypeStore store, Type adt, String name, Type tupleType) throws FactTypeDeclarationException {
    	checkNull(store, adt, name, tupleType);

    	if (!isIdentifier(name)) {
    		throw new IllegalIdentifierException(name);
    	}
     
    	Type result = getFromCache(new ConstructorType(name, (TupleType) tupleType, (AbstractDataType) adt));
    	store.declareConstructor(result);
    	return result;
    }

    /**
     * Make a new constructor type. A constructor type extends an abstract data type such
     * that it represents more values.
     * 
     * @param store    to store the declared constructor in
     * @param adt      the adt this constructor builds
     * @param name     the name of the node type
     * @param children the types of the children of the tree node type
     * @return a tree node type
     */
    public Type constructor(TypeStore store, Type adt, String name, Type... children ) throws FactTypeDeclarationException {
    	return constructorFromTuple(store, adt, name, tupleType(children));
    }
    
    /**
     * Make a new constructor type. A constructor type extends an abstract data type such
     * that it represents more values.
     * 
     * @param store    to store the declared constructor in
     * @param nodeType the type of node this constructor builds
     * @param name     the name of the node type
     * @param children the types of the children of the tree node type
     * @return a tree node type
     */
    public Type constructor(TypeStore store, Type nodeType, String name, Object... childrenAndLabels ) throws FactTypeDeclarationException { 
    	return constructorFromTuple(store, nodeType, name, tupleType(childrenAndLabels));
    }

    /**
     * Construct a list type
     * @param elementType the type of the elements in the list
     * @return a list type
     */
    public Type listType(Type elementType) {
    	checkNull(elementType);
		return (ListType) getFromCache(new ListType(elementType));
	}
    
    /**
     * Construct a map type
     * @param key    the type of the keys in the map
     * @param value  the type of the values in the map
     * @return a map type
     */
    public Type mapType(Type key, Type value) {
    	checkNull(key, value);
    	return (MapType) getFromCache(new MapType(key, value));
	}

    /** 
     * Construct a type parameter, which can later be instantiated.
     * @param name   the name of the type parameter
     * @param bound  the widest type that is acceptable when this type is instantiated
     * @return a parameter type
     */
	public Type parameterType(String name, Type bound) {
		checkNull(name, bound);
		return (ParameterType) getFromCache(new ParameterType(name, bound));
	}

    /** 
     * Construct a type parameter, which can later be instantiated.
     * @param name   the name of the type parameter
     * @return a parameter type
     */
	public Type parameterType(String name) {
		checkNull(name);
		return (ParameterType) getFromCache(new ParameterType(name));
	}

	/**
	 * Checks to see if a string is a valid PDB type, field or annotation identifier
	 * 
	 * @param str
	 * @return
	 */
	public boolean isIdentifier(String str) {
		checkNull(str);
		byte[] contents = str.getBytes();

		if (str.length() == 0) {
			return false;
		}

		if (!Character.isJavaIdentifierStart(contents[0])) {
			return false;
		}

		if (str.length() > 1) {
			for (int i = 1; i < contents.length; i++) {
				if (!Character.isJavaIdentifierPart(contents[i]) &&
					contents[i] != '.' && contents[i] != '-') {
					return false;
				}
			}
		}

		return true;
	}
}
