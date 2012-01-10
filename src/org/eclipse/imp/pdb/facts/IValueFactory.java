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

import java.net.URI;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
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
	 * Constructs an integer from the decimal representation.
	 *  
	 * @param i integer as a string of decimal digits
	 * @return
	 * @throws NumberFormatException
	 */
	public IInteger integer(String i) throws NumberFormatException ;
	
	/**
	 * @param i
	 * @return a value representing the integer i, with type IntegerType
	 */
    public IInteger integer(int i);
    
	/**
	 * @param i
	 * @return a value representing the integer i, with type IntegerType
	 */
    public IInteger integer(long i);
    
    /**
     * Construct an integer from the two's complement big-endian representation
     * @param a
     * @return a value representing the two's complement notation in a
     */
    public IInteger integer(byte[] a);

	/**
	 * @param a
	 * @param b
	 * @return a value representing the rational a/b, with type RationalType
	 */
    public IRational rational(int a, int b);

	/**
	 * @param a
	 * @param b
	 * @return a value representing the rational a/b, with type RationalType
	 */
    public IRational rational(long a, long b);

	/**
	 * @param a
	 * @param b
	 * @return a value representing the rational a/b, with type RationalType
	 */
    public IRational rational(IInteger a, IInteger b);

	/**
	 * @param rat
	 * @return a value representing the rational rat, with type RationalType
	 */
    public IRational rational(String rat) throws NumberFormatException;

    /**
     * Construct a real from the mathematical notation.
     * 
     * @param s real as a string in decimal mathematical notation.
     * @return 
     * @throws NumberFormatException
     */
    public IReal real(String s) throws NumberFormatException;
    
    /**
     * @param d
     * @return a value representing the double d, with type RealType
     */
    public IReal real(double d);
    
    
	/**
	 * @param precision max of 1000
	 * @return PI with a higher precision than standard Math.PI
	 */
	public IReal pi(int precision);
	
	/**
	 * @param precision max of 1000
	 * @return E with a higher precision than standard Math.E
	 */
	public IReal e(int precision);
    
    /**
     * @param s
     * @return a value representing the string s, with type StringType
     */
    public IString string(String s);
    
    /**
     * Build a string from an array of unicode characters.
     * @param chars array of unicode characters
     * @throws IllegalArgumentException if one of the characters in chars is not a valid codePoint
     */
    public IString string(int[] chars) throws IllegalArgumentException;
    
    /**
     * Build a string from a unicode character.
     * @param ch unicode character
     * @throws IllegalArgumentException when ch is not a valid codePoint
     */
    public IString string(int ch) throws IllegalArgumentException;
    
    /**
     * Create an exact reference to a source location.
     * 
     * @param uri         exact uri where the source is located.
     * @param offset      the character offset starting from the beginning of the file located 
     *                    at the given url. Offsets start at 0 (zero).
     * @param length      the character length of the location (the amount characters).
     * @param beginLine   the (inclusive) line number where the location begins. The first
     *                    line is always line number 1.
     * @param endLine     the (exclusive) line where the location ends
     * @param beginCol    the (inclusive) column number where the location begins. The
     *                    first column is always column number 0 (zero).
     * @param endCol      the (exclusive) column number where the location ends.
     * @return a value representing a source location, with type SourceLocationType
     */
    public ISourceLocation sourceLocation(URI uri, int offset, int length, int beginLine, int endLine, int beginCol, int endCol);
   
    /**
     * Create an exact reference to a source location.
     * 
     * @param path        exact (absolute) path where the source is located.
     * @param offset      the character offset starting from the beginning of the file located 
     *                    at the given url. Offsets start at 0 (zero).
     * @param length      the character length of the location (the amount characters).
     * @param beginLine   the (inclusive) line number where the location begins. The first
     *                    line is always line number 1.
     * @param endLine     the (exclusive) line where the location ends
     * @param beginCol    the (inclusive) column number where the location begins. The
     *                    first column is always column number 0 (zero).
     * @param endCol      the (exclusive) column number where the location ends.
     * @return a value representing a source location, with type SourceLocationType
     */
    public ISourceLocation sourceLocation(String path, int offset, int length, int beginLine, int endLine, int beginCol, int endCol);
    
    /**
     * Create an exact reference to a source location.
     * 
     * @param uri         exact uri where the source is located.
     * @return a value representing a source location, with type SourceLocationType
     */
    public ISourceLocation sourceLocation(URI uri);
   
    /**
     * Create an exact reference to a source location.
     * 
     * @param path        exact (absolute) path where the source is located.
     * @return a value representing a source location, with type SourceLocationType
     */
    public ISourceLocation sourceLocation(String path);
    
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
    public INode node(String name);
    
    /**
     * Construct a node
     * @param name     the name of the node
     * @param children the edges (children) of the node
     * @return a new tree node
     */
    public INode node(String name, IValue... children);
    
    /**
     * Construct a node
     * @param name     the name of the node
     * @param annotations to immediately put on the constructor
     * @param children an array or variable length argument list of children
     * @return a new tree value
     * @throws FactTypeUseException if the children are not of the expected types for this node type
     */
    public INode node(String name, Map<String,IValue> annotations, IValue... children) throws FactTypeUseException;
    
    /**
     * Make a nullary constructor (a typed nullary node)
     * @param constructor     the constructor to use
     * @return a new constructor value
     */
    public IConstructor constructor(Type constructor);
    
    /**
     * Make a constructor value.
     * @param constructor the constructor to use
     * @param children an array or variable length argument list of children
     * @return a new tree value
     * @throws FactTypeUseException if the children are not of the expected types for this node type
     */
    public IConstructor constructor(Type constructor, IValue... children) throws FactTypeUseException;
    
    /**
     * Make a constructor value.
     * @param constructor the constructor to use
     * @param annotations to immediately put on the constructor
     * @param children an array or variable length argument list of children
     * @return a new tree value
     * @throws FactTypeUseException if the children are not of the expected types for this node type
     */
    public IConstructor constructor(Type constructor, Map<String,IValue> annotations, IValue... children) throws FactTypeUseException;
    
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
     * Get a set writer of which the element type will be the least upper bound
     * of the element types
     * 
     * @return a set writer
     */
    public ISetWriter setWriter();
    
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
     * Get a list writer of which the element type will be the least upper bound
     * of the element types
     * 
     * @return a list writer
     */
    public IListWriter listWriter();
    
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
    public IRelation relation(Type tupleType);
    
    /**
     * Constructs a relation writer, using the provided tuple type as a schema
     * @param type of type TupleType &lt;t1,...,tn&gt;
     * @return an empty relation of type RelationType rel[t1,...,tn]
     */
    public IRelationWriter relationWriter(Type type);
    
    /**
     * Constructs a relation writer, which infers its type from the tuples given while
     * writing to the relation.
     */
    public IRelationWriter relationWriter();
    
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
     * Get a map writer of which the key and value types will be the least upper bound
     * of the keys and values that are put in.
     * 
     * @return a list writer
     */
    public IMapWriter mapWriter();
    
	/**
	 * Create a boolean with a certain value
	 * @return a boolean
	 */
	public IBool bool(boolean value);
	
	/**
	 * Create a new DateTime representing a date with the given date fields
	 * 
	 * @param year		the year of the date
	 * @param month		the month of the date
	 * @param day		the day of the date
	 * 
	 * @return			a DateTime date with the provided year, month, and day
	 */
	public IDateTime date(int year, int month, int day);

	/**
	 * Create a new DateTime representing a time with the given time fields
	 * 
	 * @param hour				the hour of the time
	 * @param minute			the minute of the time
	 * @param second			the second of the time
	 * @param millisecond		the millisecond of the time
	 * 
	 * @return					a DateTime time with the provided hour, minute, second, and milliseconds
	 */
	public IDateTime time(int hour, int minute, int second, int millisecond);
	
	/**
	 * Create a new DateTime representing a time with the given time fields
	 * 
	 * @param hour				the hour of the time
	 * @param minute			the minute of the time
	 * @param second			the second of the time
	 * @param millisecond		the millisecond of the time
	 * @param hourOffset		the hour offset of the timezone for this time (can be negative)
	 * @param minuteOffset		the minute offset of the timezone for this time (can be negative if the hourOffset is 0)
	 * 
	 * @return					a DateTime time with the provided hour, minute, second, milliseconds, and timezone offset
	 */
	public IDateTime time(int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset);
	
	/**
	 * Create a new DateTime with the given date and time fields
	 * 
	 * @param year				the year of the date
	 * @param month				the month of the date
	 * @param day				the day of the date
	 * @param hour				the hour of the time
	 * @param minute			the minute of the time
	 * @param second			the second of the time
	 * @param millisecond		the millisecond of the time
	 * 
	 * @return					a DateTime with the values for year, month, etc provided in the parameters
	 */
	public IDateTime datetime(int year, int month, int day, int hour, int minute, int second, int millisecond);

	/**
	 * Create a new DateTime with the given date and time fields
	 * 
	 * @param year				the year of the date
	 * @param month				the month of the date
	 * @param day				the day of the date
	 * @param hour				the hour of the time
	 * @param minute			the minute of the time
	 * @param second			the second of the time
	 * @param millisecond		the millisecond of the time
	 * @param hourOffset		the hour offset of the timezone for this time (can be negative)
	 * @param minuteOffset		the minute offset of the timezone for this time (can be negative if the hourOffset is 0)
	 * 
	 * @return					a DateTime with the values for year, month, etc provided in the parameters
	 */
	public IDateTime datetime(int year, int month, int day, int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset);
	
	/**
	 * Create a new DateTime representing the given instant.
	 * 
	 * @param instant			the instant in time, according to the Java epoch
	 * 
	 * @return					a DateTime set to the given instant in time
	 */
	public IDateTime datetime(long instant);
	
}
