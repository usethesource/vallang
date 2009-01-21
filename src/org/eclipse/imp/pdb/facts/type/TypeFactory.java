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
     * Keeps administration of declared tree node types
     */
    private final Map<Type, List<Type>> fSignatures = new HashMap<Type, List<Type>>();
    
    /**
     * Keeps administration of anonymous tree node types
     */
    private final Map<Type,Type> fAnonymousSignature = new HashMap<Type, Type>();

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
    
    public Type tupleType(Object... fieldTypesAndLabels) {
    	int N= fieldTypesAndLabels.length;
        int arity = N / 2;
		Type[] protoFieldTypes= new Type[arity];
        String[] protoFieldNames = new String[arity];
        for(int i=0; i < N; i+=2) {
            int pos = i / 2;
			protoFieldTypes[pos]= (Type) fieldTypesAndLabels[i];
            protoFieldNames[pos] = (String) fieldTypesAndLabels[i+1];
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
     * Construct a named type. Named types are subtypes of types. Note that in the near future
     * they will be type aliases.
     * @param name      the name of the type
     * @param superType the type it should be a subtype of (alias)
     * @return a named type
     * @throws TypeDeclarationException if a type with the same name but a different supertype was defined earlier as a named type of a NamedTreeType.
     */
    public Type namedType(String name, Type superType) throws TypeDeclarationException {
    	synchronized (fNamedTypes) {
    		if (!isIdentifier(name)) {
    			throw new TypeDeclarationException("This is not a valid identifier: " + name);
    		}

    		Type result = getFromCache(new NamedType(name, superType));

    		Type old = fNamedTypes.get(name);
    		if (old != null && !old.equals(result)) {
    			throw new TypeDeclarationException("Can not redeclare type " + old + " with a different type: " + superType);
    		}

    		Type tmp2 = new NamedTreeType(name);
    		synchronized (fCache) {
    			Type sort= fCache.get(tmp2);

    			if (sort != null) {
    				throw new TypeDeclarationException("Can not redeclare tree sort type " + sort + " with a named type");
    			}
    		}

    		fNamedTypes.put(name, (NamedType) result);
    		return (NamedType) result;
    	}
    }

    public Type treeType() {
    	return TreeType.getInstance();
    }
    
    /**
     * Construct a NamedTreeType, which is a kind of tree node. Each kind of tree node
     * may have different alternatives, which are TreeNodeTypes. A TreeNodeType is always a
     * sub type of a NamedTreeType. A NamedTreeType is always a sub type of value.
     * @param name the name of the tree sort
     * @return a NamedTreeType
     * @throws TypeDeclarationException when a NamedType with the same name was already declared. Redeclaration of a NamedTreeType is ignored.
     */
    public Type namedTreeType(String name) throws TypeDeclarationException {
    	synchronized (fSignatures) {
    		if (!isIdentifier(name)) {
    			throw new TypeDeclarationException("This is not a valid identifier: " + name);
    		}

    		Type old = fNamedTypes.get(name);
    		if (old != null) {
    			throw new TypeDeclarationException("Can not redeclare a named type " + old + " with a tree sort type.");
    		}

    		Type tmp = (NamedTreeType) getFromCache(new NamedTreeType(name));
    		
    		if (fSignatures.get(tmp) == null) {
    		  fSignatures.put(tmp, new LinkedList<Type>());
    		}
    		return tmp;
    	}
    }
    
    /**
     * Construct a new node type. A node type is always a subtype of a NamedTreeType. It
     * represents an alternative constructor for a specific NamedTreeType.
     * 
     * When name == null, an anonymous tree node type is constructed, which should always
     * have a single child. No more than one of these may be constructed per NamedTreeType.
     * 
     * @param nodeType the type of node this constructor builds
     * @param name     the name of the node type
     * @param children the types of the children of the tree node type
     * @return a tree node type
     * @throws TypeDeclarationException when a second anonymous tree is declared for the same NamedTreeType, or when
     *         name == null and tupleType has an arity != 1.
     */
    public Type treeNodeTypeFromTupleType(Type nodeType, String name, Type tupleType) throws TypeDeclarationException {
    	synchronized(fSignatures) {
    		List<Type> signature = fSignatures.get(nodeType);
    		if (signature == null) {
    			throw new TypeDeclarationException("Unknown named tree type: " + nodeType);
    		}

    		if (name == null) {
    			throw new TypeDeclarationException("Node name can not be null, use anonymousTreeType() instead");
    		}
    		
    		Type result = getFromCache(new TreeNodeType(name, (TupleType) tupleType, (NamedTreeType) nodeType));
    		signature.add(result);

    		return result;
    	}
    }
    
    /**
     * Construct a new node type. A node type is always a subtype of a NamedTreeType. It
     * represents an alternative constructor for a specific NamedTreeType. 
     * @param nodeType the type of node this constructor builds
     * @param name     the name of the node type
     * @param children the types of the children of the tree node type
     * @return a tree node type
     */
    public Type treeNodeType(Type nodeType, String name, Type... children ) throws TypeDeclarationException { 
    	return treeNodeTypeFromTupleType(nodeType, name, tupleType(children));
    }
    
    /**
     * Construct a new node type. A node type is always a subtype of a NamedTreeType. It
     * represents an alternative constructor for a specific NamedTreeType. 
     * @param nodeType the type of node this constructor builds
     * @param name     the name of the node type
     * @param children the types of the children of the tree node type
     * @return a tree node type
     */
    public Type treeNodeType(Type nodeType, String name, Object... childrenAndLabels ) throws TypeDeclarationException { 
    	return treeNodeTypeFromTupleType(nodeType, name, tupleType(childrenAndLabels));
    }
    
    /**
     * Construct a special kind of tree node. This tree node does not have
     * a name, always has exactly one child. It is used for serialized values
     * where one alternative for a NamedTreeType does not have a wrapping node name.
     * 
     * The argType must be a subtype of bool, int, double, string, sourceRange or sourceLoc.
     * Each NamedTreeType can only have one anonymous constructor.
     * 
     * @param sort        the sort (NamedTreeType) this constructor builds      
     * @param string      the name of the alternative (even though it will not be used)
     * @param argType     the type of the single child
     * @param label       the label of the single child
     * @return a TreeNodeType
     * @throws TypeDeclarationException when a second anonymous constructor is defined or when
     * argType is not one of integer, bool, double, string, sourceRange or sourceLocation.
     */
    public Type anonymousTreeType(Type sort, String string,
			Type argType, String label) throws TypeDeclarationException {
    	synchronized(fAnonymousSignature) {
    		Type earlierType = fAnonymousSignature.get(sort);
    		
			if (earlierType != null && !earlierType.equivalent(argType)) {
    			throw new TypeDeclarationException("Can only have one anonymous constructor per type");
    		}

    		if (!(argType.isSourceLocationType() || argType.isSourceRangeType() || argType.isBoolType() || argType.isIntegerType() || argType.isDoubleType() || argType.isStringType())) {
    			throw new TypeDeclarationException("Anonymous trees may only wrap bool, integer, double, or string values");
    		}

    		Type result = getFromCache(new TreeNodeType(null, (TupleType) TypeFactory.getInstance().tupleType(argType, label), (NamedTreeType) sort));

    		fAnonymousSignature.put(sort, result);
    		return result;
    	}
	}

    /**
     * Lookup a NamedType that was declared before by name
     * @param name the name of the type to lookup
     * @return
     */
    public Type lookupNamedType(String name) {
        return fNamedTypes.get(name);
    }
    
    /**
     * Returns all alternative ways of constructing a certain type name using
     * a tree type.
     * 
     * @param type
     * @return all tree node types that construct the given type
     */
    public List<Type> lookupTreeNodeTypes(Type type) {
    	List<Type> signature = fSignatures.get(type);
    	Type anonymous = fAnonymousSignature.get(type);
    	if (anonymous != null) {
    		signature.add(anonymous);
    	}
    	return signature;
    }
    
    /**
     * Lookup a TreeNodeType by name, and in the context of a certain NamedTreeType
     * @param type             the NamedTreeType context
     * @param constructorName  the name of the TreeNodeType
     * @return a TreeNodeType if it was declared before
     * @throws a FactTypeError if the type was not declared before
     */
    public List<Type> lookupTreeNodeType(Type type, String constructorName) throws FactTypeError {
    	List<Type> result = new LinkedList<Type>();
    	
    	if (constructorName == null) {
    		Type anonymous = lookupAnonymousTreeNodeType(type);
    		if (anonymous != null) {
    			result.add(anonymous);
    		}
    	}
    	else {
    		for (Type node : fSignatures.get(type)) {
    			String name = node.getName();
    			if (name.equals(constructorName)) {
    				result.add(node);
    			}
    		}
    	}

    	return result;
    }

    /**
     * Retrieve the type for an anonymous constructor.  
     * See @link {@link TypeFactory#anonymousTreeType(NamedTreeType, String, Type, String)})
     * for more information.
     * @param type NamedTreeType to lookup the constructor for
     * @return an anonymous tree node type
     * @throws FactTypeError if the type does not have an anonymous constructor
     */
    public Type lookupAnonymousTreeNodeType(Type type) throws FactTypeError {
    	return fAnonymousSignature.get(type);
    }
    
    /** 
     * Retrieve all tree node types for a given constructor name, 
     * regardless of tree sort type. If constructorName == null, it will
     * return all types that have an anonymous constructor.
     * 
     * @param constructName the name of the tree node
     */
    public List<Type> lookupTreeNodeType(String constructorName) {
    	List<Type> result = new LinkedList<Type>();
    	
    	if (constructorName != null) {
    		for (Type sort : fSignatures.keySet()) {
    			for (Type node : fSignatures.get(sort)) {
    				String name = node.getName();
    				if (name.equals(constructorName)) {
    					result.add(node);
    				}
    			}	
    		}
    	}
    	else {
    		result.addAll(fAnonymousSignature.keySet());
    	}
    	
    	return result;
    }
    
    /**
     * See if a certain tree type was declared
     * @param name  the supposed name of the named tree type
     * @return null if such type does not exist, or the type if it was declared earlier
     */
    public Type lookupNamedTreeType(String name) {
    	for (Type sort : fSignatures.keySet()) {
    		if (sort.getName().equals(name)) {
    			return sort;
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
     * @throws TypeDeclarationException when an attempt is made to define annotations for anything
     * but NamedTreeTypes orTreeNodeTypes.
     */
    public void declareAnnotation(Type onType, String key, Type valueType) {
    	if (!onType.isTreeNodeType() && !onType.isNamedTreeType()) {
    		throw new TypeDeclarationException("Can not define annotations on anything but trees");
    	}
    	
    	if (onType.isAnonymousTreeNodeType()) {
    		throw new TypeDeclarationException("Can not define annotations on anonymous tree nodes");
    	}
    	
    	synchronized (fAnnotations) {
    		Map<String, Type> annotationsForType = fAnnotations.get(onType);

    		if (!isIdentifier(key)) {
    			throw new FactTypeError("Key " + key + " is not an identifier.");
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
    			throw new TypeDeclarationException("Annotation was declared previously with different type: " + declaredEarlier.get(key));
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
    	
    	if (onType.isTreeNodeType()) {
    		localAnnotations = fAnnotations.get(((TreeNodeType) onType).getSuperType());
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
	 * @throws TypeDeclarationException if the descriptor is not a valid type descriptor
	 */
	Type fromDescriptor(IValue typeDescriptor) throws TypeDeclarationException {
		return TypeDescriptorFactory.getInstance().fromTypeDescriptor(typeDescriptor);
	}


	
}
