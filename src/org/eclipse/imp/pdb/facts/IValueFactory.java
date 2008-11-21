/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * An IValueFactory is an AbstractFactory for values. Implementations of this
 * class should guarantee that the values returned are immutable. For batch
 * construction of container classes there should be implementations of the
 * I{List,Set,Relation,Map}Writer interfaces.
 * 
 * @author jurgen@vinju.org
 * @author rfuhrer@watson.ibm.com
 *
 */
public interface IValueFactory {
	/**
	 * @param i
	 * @return a value representing the integer i, with type IntegerType
	 */
    public IInteger integer(int i);
    
    /**
     * @param d
     * @return a value representing the double d, with type DoubleType
     */
    public IDouble dubble(double d);
    
    /**
     * @param s
     * @return a value representing the string s, with type StringType
     */
    public IString string(String s);
    
    /**
     * @param startOffset
     * @param length
     * @param startLine
     * @param endLine
     * @param startCol
     * @param endCol
     * @return a value representing a source range, with type SourceRangeType
     */
    public ISourceRange sourceRange(int startOffset, int length, int startLine, int endLine, int startCol, int endCol);

    /**
     * @param path
     * @param range
     * @return a value representing a source location, with type SourceLocationType
     */
    public ISourceLocation sourceLocation(String path, ISourceRange range);
    
    /**
     * @param <T> the class of the object
     * @param o   the object itself
     * @return a value wrapping this object
     */
    public <T> IObject<T> object(T o);
    
    /**
     * Construct the nullary tuple
     * @return the nullary tuple
     */
    public ITuple tuple();
    
    /**
     * Construct a tuple
     * 
     * @param args a variable length argument list or an array of IValue
     * @return a tuple with as many children as there are args
     */
    public ITuple tuple(IValue... args);
    
    /**
     * Construct a nullary generic tree node
     * @param name the name of the tree node
     * @return a new tree value
     */
    public ITree tree(String name);
    
    /**
     * Construct a generic tree node
     * @param name     the name of the node
     * @param children the edges (children) of the node
     * @return a new tree node
     */
    public ITree tree(String name, IValue... children);
    
    /**
     * Construct a typed nullary tree node
     * @param type     the tree node type to use
     * @return a new tree value
     */
    public INode tree(TreeNodeType type);
    
    /**
     * Construct a typed tree node
     * @param type     the tree node type to use
     * @param children an array or variable length argument list of children
     * @return a new tree value
     * @throws FactTypeError if the children are not of the expected types for this node type
     */
    public INode tree(TreeNodeType type, IValue... children) throws FactTypeError;
    
    /**
     * Construct an empty unmodifiable set. If the element type is a tuple type,
     * this will actually construct a relation.
     * 
     * @param eltType type of set elements
     * @return an empty set of SetType set[eltType]
     */
    public ISet set(Type eltType);
    
    /**
     * Get a set writer for a specific kind of set. If the element type is
     * a tuple type, this will return a writer for a relation.
     * 
     * @param eltType the type of the elements of the set
     * @return a set writer
     */
    public ISetWriter setWriter(Type eltType);
    
    /**
     * Construct a set with a fixed number of elements in it. If the 
     * elements are compatible tuples, this will construct a relation.
     * 
     * @param elems an array or variable argument list of values
     * @return a set containing all the elements 
     */
    public ISet set(IValue... elems);
    
    /**
     * Construct an empty still unmodifiable list.
     * @param eltType
     * @return an empty list of ListType list[eltType]
     */
    public IList list(Type eltType);
    
    /**
     * Get a list writer for a specific kind of list
     * 
     * @param eltType the type of the elements of the list
     * @return a list writer
     */
    public IListWriter listWriter(Type eltType);
    
    /**
     * Construct a list with a fixed number of elements in it.
     * @param elems the elements to put in the list
     * @return a list [a] of type list[a.getType()]
     */
    public IList list(IValue... elems);
    
    /**
     * Constructs an new empty unmodifiable relation, using the provided tuple type as a schema
     * @param tupleType of type TupleType &lt;t1,...,tn&gt;
     * @return an empty relation of type RelationType rel[t1,...,tn]
     */
    public IRelation relation(TupleType tupleType);
    
    /**
     * Constructs a relation writer, using the provided tuple type as a schema
     * @param tupleType of type TupleType &lt;t1,...,tn&gt;
     * @return an empty relation of type RelationType rel[t1,...,tn]
     */
    public ISetWriter relationWriter(TupleType tupleType);
    
    /**
     * Construct a relation with a fixed number of tuples in it
     * @param elems an array or variable length argument list of tuples
     * @return a relation containing a number of elements
     */
    public IRelation relation(IValue... elems);
    
    /**
     * Creates an empty unmodifiable map.
     * @param key   type to use for keys
     * @param value type to use for values
     * @return an empty map
     */
	public IMap map(Type key, Type value);

	/**
	 * Create a map writer
	 * 
	 * @param key   the type of the keys in the map
	 * @param value the type of the values in the map
	 * @return a map writer
	 */
	public IMapWriter mapWriter(Type key, Type value);

	/**
	 * Create a boolean with a certain value
	 * @return a boolean
	 */
	public IBool bool(boolean value);
}
