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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.RedeclaredAnnotationException;
import org.eclipse.imp.pdb.facts.exceptions.EmptyIdentifierException;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeDeclarationException;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeRedeclaredException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalAnnotationDeclaration;
import org.eclipse.imp.pdb.facts.exceptions.IllegalFieldNameException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalFieldTypeException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalIdentifierException;
import org.eclipse.imp.pdb.facts.exceptions.RedeclaredConstructorException;
import org.eclipse.imp.pdb.facts.exceptions.RedeclaredFieldNameException;
import org.eclipse.imp.pdb.facts.exceptions.UndeclaredAbstractDataTypeException;

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
	private final Map<Type,Type> fCache = new WeakHashMap<Type,Type>();

    /**
     * Keeps administration of declared type aliases (NamedTypes)
     */
    private final Map<String, Type> fNamedTypes= new HashMap<String, Type>();
    
    /**
     * Keeps administration of declared constructor alternatives for abstract data types
     */
    private final Map<Type, List<Type>> fConstructors = new HashMap<Type, List<Type>>();
    
    /**
     * Keeps administration of declared annotations 
     */
    private final Map<Type, Map<String, Type>> fAnnotations = new HashMap<Type, Map<String, Type>>();

    public static TypeFactory getInstance() {
        return InstanceHolder.sInstance;
    }

    private TypeFactory() { }

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
    public Type doubleType() {
        return DoubleType.getInstance();
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
     * @return a reference to the unique sourceRange type of the PDB.
     */
    public Type sourceRangeType() {
        return SourceRangeType.getInstance();
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
    	return (TupleType) getFromCache(new TupleType(types.length, 0, types, labels));
    }
    
    /**
     * Construct a tuple type.
     * @param fieldTypes an array of field types in order of appearance. The array is copied.
     * @return a tuple type
     */
    public Type tupleType(IValue... elements) {
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
    	if (eltType.isTupleType()) {
    		return relTypeFromTuple(eltType);
    	}
    	else {
          return (SetType) getFromCache(new SetType(eltType));
    	}
    }

    public Type relTypeFromTuple(Type tupleType) {
    	return getFromCache(new RelationType(tupleType));
    }
    
    /**
     * Construct a relation type.
     * @param fieldTypes the types of the fields of the relation
     * @return a relation type
     */
    public Type relType(Type... fieldTypes) {
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
     * @param name      the name of the type
     * @param aliased the type it should be an alias for
     * @param parameters a list of type parameters for this alias
     * @return a named type
     * @throws FactTypeDeclarationException if a type with the same name but a different supertype was defined earlier as a named type of a AbstractDataType.
     */
    public Type aliasType(String name, Type aliased, Type...parameters) throws FactTypeDeclarationException {
    	synchronized (fNamedTypes) {
    		if (!isIdentifier(name)) {
    			throw new IllegalIdentifierException(name);
    		}
    		
    		Type paramType;
    		if (parameters.length == 0) {
    			paramType = voidType();
    		}
    		else {
    			paramType = tupleType(parameters);
    		}

    		Type result = getFromCache(new AliasType(name, aliased, paramType));

    		Type old = fNamedTypes.get(name);
    		if (old != null && !old.equals(result)) {
    			if (!result.isSubtypeOf(old)) { // we may instantiate a named type, but not redeclare it.
    				throw new FactTypeRedeclaredException(name, old);
    			}
    		}

    		Type tmp2 = new AbstractDataType(name, paramType);
    		synchronized (fCache) {
    			Type adt= fCache.get(tmp2);

    			if (adt != null) {
    				throw new FactTypeRedeclaredException(name, adt);
    			}
    		}

    		fNamedTypes.put(name, (AliasType) result);
    		return (AliasType) result;
    	}
    }
    
    public Type nodeType() {
    	return NodeType.getInstance();
    }
    
    /**
     * Construct a @{link AbstractDataType}, which is a kind of tree node. Each kind of tree node
     * may have different alternatives, which are ConstructorTypes. A @{link ConstructorType} is always a
     * sub-type of a AbstractDataType. A AbstractDataType is always a sub type of value.
     * @param name the name of the abstract data-type
     * @param parameters array of type parameters
     * @return a AbstractDataType
     * @throws FactTypeDeclarationException when a AliasType with the same name was already declared. 
     *                                  Re-declaration of an AbstractDataType is ignored.
     */
    public Type abstractDataType(String name, Type... parameters) throws FactTypeDeclarationException {
    	synchronized (fConstructors) {
    		if (!isIdentifier(name)) {
    			throw new IllegalIdentifierException(name);
    		}

    		Type old = fNamedTypes.get(name);
    		if (old != null) {
    			throw new FactTypeRedeclaredException(name, old);
    		}
    		
    		Type paramType = voidType();
    		if (parameters.length != 0) {
    			paramType = tupleType(parameters);
    		}

    		Type tmp = (AbstractDataType) getFromCache(new AbstractDataType(name, paramType));
    		
    		if (fConstructors.get(tmp) == null) {
    		  fConstructors.put(tmp, new LinkedList<Type>());
    		}
    		return tmp;
    	}
    }
    
    /**
    * Make a new constructor type. A constructor type extends an abstract data type such
     * that it represents more values.
     * 
     * @param adt the AbstractDataType this constructor builds
     * @param name     the name of the node type
     * @param children the types of the children of the tree node type
     * @return a tree node type
     * @throws FactTypeDeclarationException when a second anonymous tree is declared for the same AbstractDataType, or when
     *         name == null.
     */
    public Type constructorFromTuple(Type adt, String name, Type tupleType) throws FactTypeDeclarationException {
    	synchronized(fConstructors) {
    		List<Type> signature = fConstructors.get(adt);
    		if (signature == null) {
    			throw new UndeclaredAbstractDataTypeException(adt);
    		}

    		if (name == null || name.length() == 0) {
    			throw new EmptyIdentifierException();
    		}

    		checkOverloading(signature, name, tupleType);
    		checkFieldNames(signature, tupleType);


    		Type result = getFromCache(new ConstructorType(name, (TupleType) tupleType, (AbstractDataType) adt));
    		signature.add(result);

    		return result;
    	}
    }

	private void checkFieldNames(List<Type> signature, Type tupleType) {
		if (!tupleType.hasFieldNames()) {
			return;
		}

		for (Type alt : signature) {
			Type altArgs = alt.getFieldTypes();
			if (!altArgs.hasFieldNames()) {
				return;
			}
			for (int i = 0; i < tupleType.getArity(); i++) {
				Type type = tupleType.getFieldType(i);
				String label = tupleType.getFieldName(i);

				for (int j = 0; j < altArgs.getArity(); j++) {
					if (altArgs.getFieldName(j).equals(label)) {
						if (!altArgs.getFieldType(j).equivalent(type)) {
							throw new RedeclaredFieldNameException(label, type, altArgs.getFieldType(i));
						}
					}
				}
			}
		}
	}

	private void checkOverloading(List<Type> signature, String name,
			Type tupleType) throws FactTypeDeclarationException {
		for (Type alt : signature) {
			if (alt.isConstructorType() && alt.getName().equals(name)) {
				Type fieldTypes = alt.getFieldTypes();
				if (fieldTypes != tupleType && fieldTypes.comparable(tupleType)) {
					throw new RedeclaredConstructorException(name, fieldTypes, tupleType);
				}
			}
		}
	}
    
    /**
     * Make a new constructor type. A constructor type extends an abstract data type such
     * that it represents more values.
     * @param nodeType the type of node this constructor builds
     * @param name     the name of the node type
     * @param children the types of the children of the tree node type
     * @return a tree node type
     */
    public Type constructor(Type nodeType, String name, Type... children ) throws FactTypeDeclarationException { 
    	return constructorFromTuple(nodeType, name, tupleType(children));
    }
    
    /**
     * Make a new constructor type. A constructor type extends an abstract data type such
     * that it represents more values.
     * 
     * @param nodeType the type of node this constructor builds
     * @param name     the name of the node type
     * @param children the types of the children of the tree node type
     * @return a tree node type
     */
    public Type constructor(Type nodeType, String name, Object... childrenAndLabels ) throws FactTypeDeclarationException { 
    	return constructorFromTuple(nodeType, name, tupleType(childrenAndLabels));
    }

    /**
     * Lookup a AliasType that was declared before by name
     * @param name the name of the type to lookup
     * @return
     */
    public Type lookupAlias(String name) {
        return fNamedTypes.get(name);
    }
    
    /**
     * Returns all alternative ways of constructing a certain abstract data type.
     * 
     * @param adt
     * @return all types that construct the given type
     */
    public List<Type> lookupAlternatives(Type adt) {
    	return fConstructors.get(adt);
    }
    
    /**
     * Lookup a ConstructorType by name, and in the context of a certain AbstractDataType
     * @param adt             the AbstractDataType context
     * @param constructorName  the name of the ConstructorType
     * @return a ConstructorType if it was declared before
     * @throws a FactTypeError if the type was not declared before
     */
    public List<Type> lookupConstructor(Type adt, String constructorName) throws FactTypeUseException {
    	List<Type> result = new LinkedList<Type>();

    	List<Type> alternatives = fConstructors.get(adt);
    	if (alternatives != null) {
    		for (Type node : alternatives) {
    			String name = node.getName();
    			if (name.equals(constructorName)) {
    				result.add(node);
    			}
    		}
    	}

    	return result;
    }

    
    /** 
     * Retrieve all tree node types for a given constructor name, 
     * regardless of abstract data-type. 
     * 
     * @param constructName the name of the tree node
     */
    public List<Type> lookupConstructors(String constructorName) {
    	List<Type> result = new LinkedList<Type>();
    	
    	for (Type adt : fConstructors.keySet()) {
    		for (Type node : fConstructors.get(adt)) {
    			String name = node.getName();
    			if (name.equals(constructorName)) {
    				result.add(node);
    			}
    		}	
    	}
    	
    	return result;
    }
    
    /**
     * See if a certain tree type was declared
     * @param name  the supposed name of the named tree type
     * @return null if such type does not exist, or the type if it was declared earlier
     */
    public Type lookupAbstractDataType(String name) {
    	for (Type adt : fConstructors.keySet()) {
    		if (adt.getName().equals(name)) {
    			return adt;
    		}
    	}
    	
    	return null;
    }
    
    /**
     * Construct a list type
     * @param elementType the type of the elements in the list
     * @return a list type
     */
    public Type listType(Type elementType) {
		return (ListType) getFromCache(new ListType(elementType));
	}
    
    /**
     * Construct a map type
     * @param key    the type of the keys in the map
     * @param value  the type of the values in the map
     * @return a map type
     */
    public Type mapType(Type key, Type value) {
    	return (MapType) getFromCache(new MapType(key, value));
	}

    /** 
     * Construct a type parameter, which can later be instantiated.
     * @param name   the name of the type parameter
     * @param bound  the widest type that is acceptible when this type is instantiated
     * @return a parameter type
     */
	public Type parameterType(String name, Type bound) {
		return (ParameterType) getFromCache(new ParameterType(name, bound));
	}

    /** 
     * Construct a type parameter, which can later be instantiated.
     * @param name   the name of the type parameter
     * @return a parameter type
     */
	public Type parameterType(String name) {
		return (ParameterType) getFromCache(new ParameterType(name));
	}

    /**
     * Declare that certain tree node types may have an annotation with a certain
     * label. The annotation with that label will have a specific type.
     * 
     * @param onType the type of values that carry this annotation
     * @param key    the label of the annotation
     * @param valueType the type of values that represent the annotation
     * @throws FactTypeDeclarationException when an attempt is made to define annotations for anything
     * but NamedTreeTypes orTreeNodeTypes.
     */
    public void declareAnnotation(Type onType, String key, Type valueType) {
    	if (!onType.isConstructorType() && !onType.isAbstractDataType()) {
    		throw new IllegalAnnotationDeclaration(onType);
    	}
    	
    	synchronized (fAnnotations) {
    		Map<String, Type> annotationsForType = fAnnotations.get(onType);

    		if (!isIdentifier(key)) {
    			throw new IllegalIdentifierException(key);
    		}

    		if (annotationsForType == null) {
    			annotationsForType = new HashMap<String, Type>();
    			fAnnotations.put(onType, annotationsForType);
    		}

    		Map<String, Type> declaredEarlier = getAnnotations(onType);

    		if (!declaredEarlier.containsKey(key)) {
    			annotationsForType.put(key, valueType);
    		}
    		else if (!declaredEarlier.get(key).equals(valueType)) {
    			throw new RedeclaredAnnotationException(key, declaredEarlier.get(key));
    		}
    		// otherwise its a safe re-declaration and we do nothing
    	}
    }
    
    /**
     * Locates all declared annotations for a type, including the annotations declared
     * for all of its super types.
     * 
     * @param onType
     * @return
     */
    public Map<String, Type> getAnnotations(Type onType) {
    	Map<String, Type> result = new HashMap<String,Type>();
    	
    	Map<String, Type> localAnnotations = fAnnotations.get(onType);
    	if (localAnnotations != null) {
    	  result.putAll(localAnnotations);
    	}
    	
    	if (onType.isConstructorType()) {
    		localAnnotations = fAnnotations.get(((ConstructorType) onType).getAbstractDataType());
    		if (localAnnotations != null) {
    		  result.putAll(localAnnotations);
    		}
    	}
    	
    	return result;
    }
    
    /**
     * Retrieve the type of values that are declared to be valid for a certain kind of 
     * annotations on certain kinds of values
     * @param onType the type of values that this annotation can be found on
     * @param key    the label of the annotation to find the corresponding type of
     * @return the type of the requested annotation value or null if none exists
     */
    public Type getAnnotationType(Type onType, String key) {
    	Map<String, Type> annotationsFor = getAnnotations(onType);
    	Type result = annotationsFor.get(key);
    	
    	if (result != null) {
    		return result;
    	}
    	
    	return null;
    }

	/**
	 * Checks to see if a string is a valid PDB identifier
	 * 
	 * @param str
	 * @return
	 */
	public boolean isIdentifier(String str) {
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
					contents[i] != '.') {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Construct a type that is represented by this value. Will only work for values
	 * that have been constructed using {@link TypeDescriptorFactory#toTypeDescriptor(IValueFactory, Type)},
	 * or something that exactly mimicked it.
	 * 
	 * @param descriptor a value that represents a type
	 * @return a type that was represented by the descriptor
	 * @throws FactTypeDeclarationException if the descriptor is not a valid type descriptor
	 */
	Type fromDescriptor(IValue typeDescriptor) throws FactTypeDeclarationException {
		return TypeDescriptorFactory.getInstance().fromTypeDescriptor(typeDescriptor);
	}


	
}
