/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* Copyright (C) 2007-2013 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Anya Helene Bagge - rationals and labeled maps

*******************************************************************************/
package io.usethesource.vallang;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;

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
	 * @param i
	 *            integer as a string of decimal digits
	 * @return a new integer
	 * @throws NumberFormatException
	 */
	public IInteger integer(String i) throws NumberFormatException;

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
	 * 
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
	 * @param s
	 *            real as a string in decimal mathematical notation.
	 * @return the corresponding real
	 * @throws NumberFormatException
	 */
	public IReal real(String s) throws NumberFormatException;

	/**
	 * Construct a real from the mathematical notation.
	 * 
	 * @param s
	 *            real as a string in decimal mathematical notation.
	 * @param p
	 *            precision
	 * @return the corresponding real
	 * @throws NumberFormatException
	 */
	public IReal real(String s, int p) throws NumberFormatException;

	/**
	 * @param d
	 * @return a value representing the double d, with type RealType
	 */
	public IReal real(double d);

	/**
	 * @param d
	 * @param p
	 *            precision
	 * @return a value representing the double d, with type RealType
	 */
	public IReal real(double d, int p);

	/**
	 * @return the global precision for reals
	 */
	public int getPrecision();

	/**
	 * Set the global precision for reals
	 * 
	 * @param p
	 * @return the previous global precision
	 */
	public int setPrecision(int p);

	/**
	 * @param precision
	 *            (max of 1000)
	 * @return PI with a higher precision than standard Math.PI
	 */
	public IReal pi(int precision);

	/**
	 * @param precision
	 *            (max of 1000)
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
	 * 
	 * @param chars
	 *            array of unicode characters
	 * @throws IllegalArgumentException
	 *             if one of the characters in chars is not a valid codePoint
	 */
	public IString string(int[] chars) throws IllegalArgumentException;

	/**
	 * Build a string from a unicode character.
	 * 
	 * @param ch
	 *            unicode character
	 * @throws IllegalArgumentException
	 *             when ch is not a valid codePoint
	 */
	public IString string(int ch) throws IllegalArgumentException;

	/**
	 * Create an exact reference to a source location.
	 * 
	 * @param uri
	 *            exact uri where the source is located.
	 * @param offset
	 *            the character offset starting from the beginning of the file
	 *            located at the given url. Offsets start at 0 (zero).
	 * @param length
	 *            the character length of the location (the amount characters).
	 * @param beginLine
	 *            the (inclusive) line number where the location begins. The
	 *            first line is always line number 1.
	 * @param endLine
	 *            the (exclusive) line where the location ends
	 * @param beginCol
	 *            the (inclusive) column number where the location begins. The
	 *            first column is always column number 0 (zero).
	 * @param endCol
	 *            the (exclusive) column number where the location ends.
	 * @return a value representing a source location, with type
	 *         SourceLocationType
	 * @deprecated please use the overload with a ISourceLocation
	 */
	@Deprecated
	public ISourceLocation sourceLocation(URI uri, int offset, int length,
			int beginLine, int endLine, int beginCol, int endCol);

	/**
	 * Create an exact reference to a source location.
	 * 
	 * @param loc
	 *            where the source is located. (only the location part of the source location is used)
	 * @param offset
	 *            the character offset starting from the beginning of the file
	 *            located at the given url. Offsets start at 0 (zero).
	 * @param length
	 *            the character length of the location (the amount characters).
	 * @param beginLine
	 *            the (inclusive) line number where the location begins. The
	 *            first line is always line number 1.
	 * @param endLine
	 *            the (exclusive) line where the location ends
	 * @param beginCol
	 *            the (inclusive) column number where the location begins. The
	 *            first column is always column number 0 (zero).
	 * @param endCol
	 *            the (exclusive) column number where the location ends.
	 * @return a value representing a source location, with type
	 *         SourceLocationType
	 */
	public ISourceLocation sourceLocation(ISourceLocation loc, int offset,
			int length, int beginLine, int endLine, int beginCol, int endCol);

	/**
	 * Create an exact reference to a source location.
	 * 
	 * @param uri
	 *            exact uri where the source is located.
	 * @param offset
	 *            the character offset starting from the beginning of the file
	 *            located at the given url. Offsets start at 0 (zero).
	 * @param length
	 *            the character length of the location (the amount characters).
	 * 
	 * @return a value representing a source location, with type
	 *         SourceLocationType
	 * @deprecated please use the overload with a ISourceLocation
	 */
	@Deprecated
	public ISourceLocation sourceLocation(URI uri, int offset, int length);

	/**
	 * Create an exact reference to a source location.
	 * 
	 * @param loc
	 *            where the source is located. (only the location part of the source location is used)
	 * @param offset
	 *            the character offset starting from the beginning of the file
	 *            located at the given url. Offsets start at 0 (zero).
	 * @param length
	 *            the character length of the location (the amount characters).
	 * 
	 * @return a value representing a source location, with type
	 *         SourceLocationType
	 */
	public ISourceLocation sourceLocation(ISourceLocation loc, int offset,
			int length);

	/**
	 * Create an exact reference to a source location.
	 * 
	 * @param path
	 *            exact (absolute) path where the source is located.
	 * @param offset
	 *            the character offset starting from the beginning of the file
	 *            located at the given url. Offsets start at 0 (zero).
	 * @param length
	 *            the character length of the location (the amount characters).
	 * @param beginLine
	 *            the (inclusive) line number where the location begins. The
	 *            first line is always line number 1.
	 * @param endLine
	 *            the (exclusive) line where the location ends
	 * @param beginCol
	 *            the (inclusive) column number where the location begins. The
	 *            first column is always column number 0 (zero).
	 * @param endCol
	 *            the (exclusive) column number where the location ends.
	 * @return a value representing a source location, with type
	 *         SourceLocationType
	 */
	public ISourceLocation sourceLocation(String path, int offset, int length,
			int beginLine, int endLine, int beginCol, int endCol);

	/**
	 * Create an exact reference to a source location.
	 * 
	 * @param uri
	 *            exact uri where the source is located.
	 * @return a value representing a source location, with type
	 *         SourceLocationType
	 */
	public ISourceLocation sourceLocation(URI uri);

	/**
	 * Create an exact reference to a source location
	 * 
	 * note that we assume non-encoded input
	 * 
	 * @param scheme
	 *            the scheme part of an source location
	 * @param authority
	 *            the authority part of an source location
	 * @param path
	 *            the path part of an source location
	 * @return
	 */
	public ISourceLocation sourceLocation(String scheme, String authority,
			String path) throws URISyntaxException;

	/**
	 * Create an exact reference to a source location
	 * 
	 * note that we assume non-encoded input
	 * 
	 * @param scheme
	 *            the scheme part of an source location
	 * @param authority
	 *            the authority part of an source location
	 * @param path
	 *            the path part of an source location
	 * @param query
	 *            the query part of an source location
	 * @param fragment
	 *            the fragment part of an source location
	 * @return
	 */
	public ISourceLocation sourceLocation(String scheme, String authority,
			String path, String query, String fragment)
			throws URISyntaxException;

	/**
	 * Create an exact reference to a source location.
	 * 
	 * @param path
	 *            exact (absolute) path where the source is located.
	 * @return a value representing a source location, with type
	 *         SourceLocationType
	 */
	public ISourceLocation sourceLocation(String path);

	/**
	 * Construct the nullary tuple
	 * 
	 * @return the nullary tuple
	 */
	public ITuple tuple();

	/**
	 * Construct a tuple
	 * 
	 * @param args
	 *            a variable length argument list or an array of IValue
	 * @return a tuple with as many children as there are args
	 */
	public ITuple tuple(IValue... args);

	/**
	 * Construct a tuple of the given TupleType
	 * 
	 * The length of the argument list must match the number of children in the
	 * tuple type. Use this method if you need to create tuples with labeled
	 * children.
	 * 
	 * @param args
	 *            a variable length argument list or an array of IValue
	 * @return a tuple with as many children as there are args
	 * @deprecated will be replaced by tuple(IValue ... arg).checkBounds(Type
	 *             ... types)
	 */
	@Deprecated
	public ITuple tuple(Type type, IValue... args);

	/**
	 * Construct a nullary generic tree node
	 * 
	 * @param name
	 *            the name of the tree node
	 * @return a new tree value
	 */
	public INode node(String name);

	/**
	 * Construct a node
	 * 
	 * @param name
	 *            the name of the node
	 * @param children
	 *            the edges (children) of the node
	 * @return a new tree node
	 */
	public INode node(String name, IValue... children);

	/**
	 * Construct a node
	 * 
	 * @param name
	 *            the name of the node
	 * @param annotations
	 *            to immediately put on the constructor
	 * @param children
	 *            an array or variable length argument list of children
	 * @return a new tree value
	 * @throws FactTypeUseException
	 *             if the children are not of the expected types for this node
	 *             type
	 */
	public INode node(String name, Map<String, IValue> annotations,
			IValue... children) throws FactTypeUseException;

	/**
	 * Construct a node with keyword arguments
	 * 
	 * @param name
	 *            the name of the node
	 * @param annotations
	 *            to immediately put on the constructor
	 * @param keyArgValues
	 *            the keyword parameters with their values
	 * @param children
	 *            an array or variable length argument list of children
	 * @return a new tree value
	 * @throws FactTypeUseException
	 *             if the children are not of the expected types for this node
	 *             type
	 */
	public INode node(String name, IValue[] children,
			Map<String, IValue> keyArgValues) throws FactTypeUseException;

	/**
	 * Make a nullary constructor (a typed nullary node)
	 * 
	 * @param constructor
	 *            the constructor to use
	 * @return a new constructor value
	 */
	public IConstructor constructor(Type constructor);

	/**
	 * Make a constructor value.
	 * 
	 * @param constructor
	 *            the constructor to use
	 * @param children
	 *            an array or variable length argument list of children
	 * @return a new tree value
	 * @throws FactTypeUseException
	 *             if the children are not of the expected types for this node
	 *             type
	 */
	public IConstructor constructor(Type constructor, IValue... children)
			throws FactTypeUseException;

	/**
	 * Make a constructor value.
	 * 
	 * @param constructor
	 *            the constructor to use
	 * @param annotations
	 *            to immediately put on the constructor
	 * @param children
	 *            an array or variable length argument list of children
	 * @return a new tree value
	 * @throws FactTypeUseException
	 *             if the children are not of the expected types for this node
	 *             type
	 * @deprecated annotations will be replaced by keyword parameters
	 */
	@Deprecated
	public IConstructor constructor(Type constructor,
			Map<String, IValue> annotations, IValue... children)
			throws FactTypeUseException;
	
	 /**
   * Make a constructor value with keyword parameters
   * 
   * @param constructor
   *            the constructor to use
   * @param children
   *            an array or variable length argument list of children
   * @param kwParams keyword parameters
   * @return a new tree value
   * @throws FactTypeUseException
   *             if the children are not of the expected types for this node
   *             type
   */
  public IConstructor constructor(Type constructor, IValue[] children, Map<String, IValue> kwParams)
      throws FactTypeUseException;

	/**
	 * Get a set writer of which the element type will be the least upper bound
	 * of the element types
	 * 
	 * @return a set writer
	 */
	public ISetWriter setWriter();

	/**
	 * Construct a set with a fixed number of elements in it. If the elements
	 * are compatible tuples, this will construct a relation.
	 * 
	 * @param elems
	 *            an array or variable argument list of values
	 * @return a set containing all the elements
	 */
	public ISet set(IValue... elems);

	/**
	 * Get a list writer of which the element type will be the least upper bound
	 * of the element types
	 * 
	 * @return a list writer
	 */
	public IListWriter listWriter();

	/**
	 * Construct a list with a fixed number of elements in it.
	 * 
	 * @param elems
	 *            the elements to put in the list
	 * @return a list [a] of type list[a.getType()]
	 */
	public IList list(IValue... elems);

	/**
	 * Get a map writer of which the key and value types will be the least upper
	 * bound of the keys and values that are put in.
	 * 
	 * @return a list writer
	 */
	public IMapWriter mapWriter();
	
	/**
	 * @return an empty map
	 */
	public default IMap map() {
	    return mapWriter().done();
	}

	/**
	 * Create a boolean with a certain value
	 * 
	 * @return a boolean
	 */
	public IBool bool(boolean value);

	/**
	 * Create a new DateTime representing a date with the given date fields
	 * 
	 * @param year
	 *            the year of the date
	 * @param month
	 *            the month of the date
	 * @param day
	 *            the day of the date
	 * 
	 * @return a DateTime date with the provided year, month, and day
	 */
	public IDateTime date(int year, int month, int day);

	/**
	 * Create a new DateTime representing a time with the given time fields
	 * 
	 * @param hour
	 *            the hour of the time
	 * @param minute
	 *            the minute of the time
	 * @param second
	 *            the second of the time
	 * @param millisecond
	 *            the millisecond of the time
	 * 
	 * @return a DateTime time with the provided hour, minute, second, and
	 *         milliseconds
	 */
	public IDateTime time(int hour, int minute, int second, int millisecond);

	/**
	 * Create a new DateTime representing a time with the given time fields
	 * 
	 * @param hour
	 *            the hour of the time
	 * @param minute
	 *            the minute of the time
	 * @param second
	 *            the second of the time
	 * @param millisecond
	 *            the millisecond of the time
	 * @param hourOffset
	 *            the hour offset of the timezone for this time (can be
	 *            negative)
	 * @param minuteOffset
	 *            the minute offset of the timezone for this time (can be
	 *            negative if the hourOffset is 0)
	 * 
	 * @return a DateTime time with the provided hour, minute, second,
	 *         milliseconds, and timezone offset
	 */
	public IDateTime time(int hour, int minute, int second, int millisecond,
			int hourOffset, int minuteOffset);

	/**
	 * Create a new DateTime with the given date and time fields
	 * 
	 * @param year
	 *            the year of the date
	 * @param month
	 *            the month of the date
	 * @param day
	 *            the day of the date
	 * @param hour
	 *            the hour of the time
	 * @param minute
	 *            the minute of the time
	 * @param second
	 *            the second of the time
	 * @param millisecond
	 *            the millisecond of the time
	 * 
	 * @return a DateTime with the values for year, month, etc provided in the
	 *         parameters
	 */
	public IDateTime datetime(int year, int month, int day, int hour,
			int minute, int second, int millisecond);

	/**
	 * Create a new DateTime with the given date and time fields
	 * 
	 * @param year
	 *            the year of the date
	 * @param month
	 *            the month of the date
	 * @param day
	 *            the day of the date
	 * @param hour
	 *            the hour of the time
	 * @param minute
	 *            the minute of the time
	 * @param second
	 *            the second of the time
	 * @param millisecond
	 *            the millisecond of the time
	 * @param hourOffset
	 *            the hour offset of the timezone for this time (can be
	 *            negative)
	 * @param minuteOffset
	 *            the minute offset of the timezone for this time (can be
	 *            negative if the hourOffset is 0)
	 * 
	 * @return a DateTime with the values for year, month, etc provided in the
	 *         parameters
	 */
	public IDateTime datetime(int year, int month, int day, int hour,
			int minute, int second, int millisecond, int hourOffset,
			int minuteOffset);

	/**
	 * Create a new DateTime representing the given instant.
	 * 
	 * @param instant
	 *            the instant in time, according to the Java epoch
	 * 
	 * @return a DateTime set to the given instant in time
	 */
	public IDateTime datetime(long instant);

	/**
	 * Create a new DateTime representing the given instant.
	 * 
	 * @param instant
	 *            the instant in time, according to the Java epoch
	 * @param timezoneHours The hour offset for the new object's timezone 
	 * @param timezoneMinutes The minute offset for the new object's timezone
	 * 
	 * @return a DateTime set to the given instant in time
	 */
	public IDateTime datetime(long instant, int timezoneHours, int timezoneMinutes);
	
}
