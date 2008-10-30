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

import java.util.List;

import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;

/**
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
    
    public IInteger integer(NamedType type, int i) throws FactTypeError;

    /**
     * @param d
     * @return a value representing the double d, with type DoubleType
     */
    public IDouble dubble(double d);
    
    public IDouble dubble(NamedType type, double d) throws FactTypeError;

    /**
     * @param s
     * @return a value representing the string s, with type StringType
     */
    public IString string(String s);
    
    public IString string(NamedType type, String s) throws FactTypeError;

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

    public ISourceRange sourceRange(NamedType type, int startOffset, int length, int startLine, int endLine, int startCol, int endCol) throws FactTypeError;

    /**
     * @param path
     * @param range
     * @return a value representing a source location, with type SourceLocationType
     */
    public ISourceLocation sourceLocation(String path, ISourceRange range);
    
    public ISourceLocation sourceLocation(NamedType type, String path, ISourceRange range) throws FactTypeError;

    /**
     * @param <T> the class of the object
     * @param o   the object itself
     * @return a value wrapping this object
     */
    public <T> IObject<T> object(T o);
    
    public <T> IObject<T> object(NamedType type, T o) throws FactTypeError;
    
    /**
     * @param a
     * @return a singleton tuple &lt;a&gt;, with TupleType &lt;a.geType()&gt;
     */
    public ITuple tuple(IValue a);
    
    /**
     * @return a tuple &lt;a,b&gt; with TupleType &lt;a.getType(),b.getType()&gt;
     */
    public ITuple tuple(IValue a, IValue b);
    
    /**
     * @return a tuple &lt;a,b,c&gt; with TupleType &lt;a.getType(),b.getType(),c.getType()&gt;
     */
    public ITuple tuple(IValue a, IValue b, IValue c);
    
    /**
     * @return a tuple &lt;a,b,c,d&gt; with TupleType &lt;a.getType(),b.getType(),c.getType(),d.getType()&gt;
     */
    public ITuple tuple(IValue a, IValue b, IValue c, IValue d);
    
    /**
     * @return a tuple &lt;a,b,c,d,e&gt; with TupleType &lt;a.getType(),b.getType(),c.getType(),d.getType(),e.getType()&gt;
     */
    public ITuple tuple(IValue a, IValue b, IValue c, IValue d, IValue e);
    
    /**
     * @return a tuple &lt;a,b,c,d,e,f&gt; with TupleType &lt;a.getType(),b.getType(),c.getType(),d.getType(),e.getType(),f.getType()&gt;
     */
    public ITuple tuple(IValue a, IValue b, IValue c, IValue d, IValue e, IValue f);
    
    /**
     * @return a tuple &lt;a,b,c,d,e,f,g&gt; with TupleType &lt;a.getType(),b.getType(),c.getType(),d.getType(),e.getType(),f.getType(),g.getType()&gt;
     */
    public ITuple tuple(IValue a, IValue b, IValue c, IValue d, IValue e, IValue f, IValue g);

    /**
     * @param elements an array that contains the elements of the tuple
     * @param size the number of elements to read from the array to construct a tuple
     * @return a tuple as wide as the size of the value array. Use this method with care,
     * this method will duplicate the array and can be expensive.
     */
    public ITuple tuple(IValue[] elements, int size);
    
    public ITuple tuple(NamedType type, IValue[] elements, int size);
    
    /**
     * construct a tree node
     * 
     * @param type     describes the type of the node, its name and its children
     * @param children the children of the node
     * @return a tree 
     */
    public ITree tree(TreeNodeType type, IValue[] children) throws FactTypeError;
    public ITree tree(NamedType type, IValue[] children)  throws FactTypeError;
    public IValue tree(TreeNodeType type, List<IValue> children)  throws FactTypeError;
    
    public ITree tree(TreeNodeType type) ;
    public ITree tree(TreeNodeType type, IValue child1)  throws FactTypeError;
    public ITree tree(TreeNodeType type, IValue child1, IValue child2)  throws FactTypeError;
    public ITree tree(TreeNodeType type, IValue child1, IValue child2, IValue child3)  throws FactTypeError;
    public ITree tree(TreeNodeType type, IValue child1, IValue child2, IValue child3, IValue child4)  throws FactTypeError;
    public ITree tree(TreeNodeType type, IValue child1, IValue child2, IValue child3, IValue child4, IValue child5)  throws FactTypeError;
    public ITree tree(TreeNodeType type, IValue child1, IValue child2, IValue child3, IValue child4, IValue child5, IValue child6)  throws FactTypeError;
    public ITree tree(TreeNodeType type, IValue child1, IValue child2, IValue child3, IValue child4, IValue child5, IValue child6, IValue child7) throws FactTypeError;
    
    public ITree tree(NamedType type);
    public ITree tree(NamedType type, IValue child1)  throws FactTypeError;
    public ITree tree(NamedType type, IValue child1, IValue child2)  throws FactTypeError;
    public ITree tree(NamedType type, IValue child1, IValue child2, IValue child3)  throws FactTypeError;
    public ITree tree(NamedType type, IValue child1, IValue child2, IValue child3, IValue child4)  throws FactTypeError;
    public ITree tree(NamedType type, IValue child1, IValue child2, IValue child3, IValue child4, IValue child5)  throws FactTypeError;
    public ITree tree(NamedType type, IValue child1, IValue child2, IValue child3, IValue child4, IValue child5, IValue child6)  throws FactTypeError;
    public ITree tree(NamedType type, IValue child1, IValue child2, IValue child3, IValue child4, IValue child5, IValue child6, IValue child7)  throws FactTypeError;
    
    /**
     * @param setType the type of the set
     * @return an empty set of type setType
     * @throws FactTypeError if setType is not a subtype of SetType
     */
    public ISet set(NamedType setType) throws FactTypeError;
    
    /**
     * @param eltType type of set elements
     * @return an empty set of SetType set[eltType]
     */
    public ISet set(Type eltType);
    
    /**
     * @return a singleton set of SetType set[a.getType()]
     */
    public ISet setWith(IValue a);
    
    /**
     * @return a set {a, b} of type set[lub(a,b)]
     */
    public ISet setWith(IValue a, IValue b);
    
    /**
     * @return a set {a, b, c} of type set[lub(a.getType(),b.getType(),c.getType())]
     */
    public ISet setWith(IValue a, IValue b, IValue c);
    
    /**
     * @return a set {a, b, c, d} of type set[lub(a.getType(),b.getType(),c.getType(),d.getType())]
     */
    public ISet setWith(IValue a, IValue b, IValue c, IValue d);
    
    
    /**
     * @return a set {a, b, c, d, e} of type set[lub(a.getType(),b.getType(),c.getType(),d.getType(),e.getType())]
     */
    public ISet setWith(IValue a, IValue b, IValue c, IValue d, IValue e);
    
    /**
     * @return a set {a, b, c, d, e, f} of type set[lub(a.getType(),b.getType(),c.getType(),d.getType(),e.getType(), f.getType())]
     */
    public ISet setWith(IValue a, IValue b, IValue c, IValue d, IValue e, IValue f);
    
    /**
     * @return a set {a, b, c, d, e, f, g} of type set[lub(a.getType(),b.getType(),c.getType(),d.getType(),e.getType(), f.getType(), g.getType())]
     */
    public ISet setWith(IValue a, IValue b, IValue c, IValue d, IValue e, IValue f, IValue g);

    /**
     * @param listType  the type of the list 
     * @return an empty list of type listType
     * @throws FactTypeError if listType is not a subtype of ListType
     */
    public IList list(NamedType listType) throws FactTypeError;
    
    /**
     * @param eltType
     * @return an empty list of ListType list[eltType]
     */
    public IList list(Type eltType);
    
    /**
     * @return a list [a] of type list[a.getType()]
     */
    public IList listWith(IValue a);
    
    /**
     * @return a list [a, b] of type list[lub(a.getType(),b.getType())]
     */
    public IList listWith(IValue a, IValue b);
    
    /**
     * @return a list [a, b, c] of type list[lub(a.getType(),b.getType(),c.getType())]
     */
    public IList listWith(IValue a, IValue b, IValue c);
    
    /**
     * @return a list [a, b, c, d] of type list[lub(a.getType(),b.getType(),c.getType(),d.getType())]
     */
    public IList listWith(IValue a, IValue b, IValue c, IValue d);
    
    /**
     * @return a list [a, b, c, d, e] of type list[lub(a.getType(),b.getType(),c.getType(),d.getType(),e.getType())]
     */
    public IList listWith(IValue a, IValue b, IValue c, IValue d, IValue e);
    
    /**
     * @return a list [a, b, c, d, e, f] of type list[lub(a.getType(),b.getType(),c.getType(),d.getType(),e.getType(), f.getType())]
     */
    public IList listWith(IValue a, IValue b, IValue c, IValue d, IValue e, IValue f);
    
    /**
     * @return a list [a, b, c, d, e, f, g] of type list[lub(a.getType(),b.getType(),c.getType(),d.getType(),e.getType(), f.getType(), g.getType())]
     */
    public IList listWith(IValue a, IValue b, IValue c, IValue d, IValue e, IValue f, IValue g);

    /**
     * @param relType  the type of the relation 
     * @return an empty relation of type relationType
     * @throws FactTypeError if relType is not a subtype of RelationType
     */
    public IRelation relation(NamedType relType) throws FactTypeError;
    
    /**
     * Constructs an new relation, using the provided tuple type as a schema
     * @param tupleType of type TupleType &lt;t1,...,tn&gt;
     * @return an empty relation of type RelationType rel[t1,...,tn]
     */
    public IRelation relation(TupleType tupleType);
    
    /**
     * @param a a tuple with TupleType &lt;t1,...,tn&gt;
     * @return a relation containing one element, {a} with type RelationType rel[t1,...,tn]
     */
    public IRelation relationWith(ITuple a);
    
    /**
     * Computes the lub of a and b as TupleType &lt;t1,...tn&gt, and builds a new relation
     * @return a relation containing two tuple {a , b} with type RelationType[t1,...,tn]
     * @throws FactTypeError if the arities of the tuples are not equal
     */
    public IRelation relationWith(ITuple a, ITuple b)  throws FactTypeError;
    
    /**
     * Computes the lub of the types of a,b,c as TupleType &lt;t1,...tn&gt, and builds a new relation
     * @return a relation containing the tuple {a , b, c} with type RelationType[t1,...,tn]
     * @throws FactTypeError if the arities of the tuples are not equal
     */
    public IRelation relationWith(ITuple a, ITuple b, ITuple c)  throws FactTypeError;
    
    /**
     * Computes the lub of the types of a,b,c,d as TupleType &lt;t1,...tn&gt, and builds a new relation
     * @return a relation containing the tuple {a , b, c, d} with type RelationType[t1,...,tn]
     * @throws FactTypeError if the arities of the tuples are not equal
     */
    public IRelation relationWith(ITuple a, ITuple b, ITuple c, ITuple d)  throws FactTypeError;
    
    /**
     * Computes the lub of the types of a,b,c,d,e as TupleType &lt;t1,...tn&gt, and builds a new relation
     * @return a relation containing the tuple {a , b, c, d, e} with type RelationType[t1,...,tn]
     * @throws FactTypeError if the arities of the tuples are not equal
     */
    public IRelation relationWith(ITuple a, ITuple b, ITuple c, ITuple d, ITuple e)  throws FactTypeError;
    
    /**
     * Computes the lub of the types of a,b,c,d,e,f as TupleType &lt;t1,...tn&gt, and builds a new relation
     * @return a relation containing the tuple {a , b, c, d, e, f} with type RelationType[t1,...,tn]
     * @throws FactTypeError if the arities of the tuples are not equal
     */
    public IRelation relationWith(ITuple a, ITuple b, ITuple c, ITuple d, ITuple e, ITuple f)  throws FactTypeError;
    
    /**
     * Computes the lub of the types of a,b,c,d,e,f,g as TupleType &lt;t1,...tn&gt, and builds a new relation
     * @return a relation containing the tuple {a , b, c, d, e, f, g} with type RelationType[t1,...,tn]
     * @throws FactTypeError if the arities of the tuples are not equal
     */
    public IRelation relationWith(ITuple a, ITuple b, ITuple c, ITuple d, ITuple e, ITuple f, ITuple g)  throws FactTypeError;

    /**
     * Creates an empty map.
     * @param key   type to use for keys
     * @param value type to use for values
     * @return an empty map
     */
	public IMap map(Type key, Type value);
	
	/**
     * Creates an empty map.
     * @param type the type of the map (must be a subtype of MapType)
     * @return an empty map
     */
	public IMap map(NamedType type);
}
