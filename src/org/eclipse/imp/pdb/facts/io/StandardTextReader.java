/*******************************************************************************
 * Copyright (c) CWI 2008 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jurgen Vinju (jurgenv@cwi.nl) - initial API and implementation

 *******************************************************************************/

package org.eclipse.imp.pdb.facts.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.IWriter;
import org.eclipse.imp.pdb.facts.exceptions.FactParseError;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.OverloadingNotSupportedException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;

/**
 * This class implements the standard readable syntax for {@link IValue}'s.
 * Note that the parser also validates the input according to a given {@link Type}.
 * 
 * Note however that overloaded constructors for abstract data-types are <b>not</b> supported.
 * 
 * See also {@link StandardTextWriter}
 */
public class StandardTextReader extends AbstractReader {

	private static final char START_OF_LOC = '|';
	private static final char START_OF_STRING = '\"';
	private static final char END_OF_STRING = '\"';
	private static final char START_OF_MAP = '(';
	private static final char START_OF_ARGUMENTS = '(';
	private static final char END_OF_ARGUMENTS = ')';
	private static final char START_OF_TUPLE = '<';
	private static final char START_OF_SET = '{';
	private static final char START_OF_LIST = '[';
	private static final char END_OF_TUPLE = '>';
	private static final char COMMA_SEPARATOR = ',';
	private static final char END_OF_MAP = ')';
	private static final char DOUBLE_DOT = '.';
	private static final char END_OF_SET = '}';
	private static final char END_OF_LIST = ']';
	private static final char END_OF_LOCATION = '|';
	private static final char START_OF_DATETIME = '$';
	
	
	private TypeStore store;
	private NoWhiteSpaceInputStream stream;
	private IValueFactory factory;
	private TypeFactory types;
	private int current;

	@Override
	public IValue read(IValueFactory factory, TypeStore store, Type type,
			InputStream stream) throws FactTypeUseException, IOException {
		this.store = store;
		this.stream = new NoWhiteSpaceInputStream(stream);
		this.factory = factory;
		this.types = TypeFactory.getInstance();

		current = stream.read();
		return readValue(type);
	}

	private IValue readValue(Type expected) throws IOException {
		IValue result = null;
		
		if (Character.isDigit(current)) {
			result = readNumber(expected);
		} 
		else if ((Character.isJavaIdentifierStart(current) && '$' != current)
				|| current == '\\') {
			result = readNode(expected);
		}
		else {
			switch (current) {
			case START_OF_STRING:
				result = readString(expected);
				break;
			case START_OF_LIST:
				result = readList(expected);
				break;
			case START_OF_SET:
				result = readSet(expected);
				break;
			case START_OF_TUPLE:
				result = readTuple(expected);
				break;
			case START_OF_MAP:
				result = readMap(expected);
				break;
			case START_OF_LOC:
				result = readLocation(expected);
				break;
			case START_OF_DATETIME:
				result = readDateTime(expected);
				break;
			default:
				unexpected();
			}
		}
		
		if (!result.getType().isSubtypeOf(expected)) {
			throw new UnexpectedTypeException(expected, result.getType());
		}
		
		if (current == '[') {
			if (result.getType().isSubtypeOf(types.nodeType())) {
				result = readAnnos(expected, (INode) result);
			}
			else {
				unexpected(']');
			}
		}
		
		return result;
	}

	private IValue readLocation(Type expected) throws IOException {
		try {

			String url = parseURL();
			if (current == START_OF_ARGUMENTS) {
				ArrayList<IValue> args = new ArrayList<IValue>(4);
				readFixed(types.valueType(), ')', args);

				if (!args.get(0).getType().isSubtypeOf(types.integerType())) {
					throw new UnexpectedTypeException(types.integerType(), args.get(0).getType());
				}
				if (!args.get(1).getType().isSubtypeOf(types.integerType())) {
					throw new UnexpectedTypeException(types.integerType(), args.get(1).getType());
				}
				Type posType = types.tupleType(types.integerType(), types.integerType());

				if (!args.get(2).getType().isSubtypeOf(posType)) {
					throw new UnexpectedTypeException(posType, args.get(2).getType());
				}
				if (!args.get(3).getType().isSubtypeOf(posType)) {
					throw new UnexpectedTypeException(posType, args.get(3).getType());
				}

				int offset = Integer.parseInt(args.get(0).toString());
				int length = Integer.parseInt(args.get(1).toString());
				int beginLine = Integer.parseInt(((ITuple) args.get(2)).get(0).toString());
				int beginColumn = Integer.parseInt(((ITuple) args.get(2)).get(1).toString());
				int endLine = Integer.parseInt(((ITuple) args.get(3)).get(0).toString());
				int endColumn = Integer.parseInt(((ITuple) args.get(3)).get(1).toString());


				return factory.sourceLocation(new URI(url), offset, length, beginLine, endLine, beginColumn, endColumn);
			}

			return factory.sourceLocation(new URI(url));

		} catch (URISyntaxException e) {
			throw new FactParseError(e.getMessage(), stream.offset, e);
		}
	}

	private String parseURL() throws IOException {
		current = stream.read();
		StringBuilder result = new StringBuilder();
		
		while (current != END_OF_LOCATION) {
			result.append((char) current);
			current = stream.read();
			if (current == -1) {
				unexpected();
			}
		}
		
		current = stream.read();
		
		return result.toString();
	}

	private IValue readMap(Type expected) throws IOException {
		Type keyType = expected.isMapType() ? expected.getKeyType() : types.valueType();
		Type valueType = expected.isMapType() ? expected.getValueType() : types.valueType();
		IMapWriter w = expected.isMapType() ? factory.mapWriter(keyType, valueType) : factory.mapWriter();

		current = stream.read();
		
		while (current != END_OF_MAP) {
			IValue key = readValue(keyType);
			checkAndRead(':');
			IValue value = readValue(valueType);
			w.put(key, value);
			
			if (current != COMMA_SEPARATOR || current == END_OF_MAP) {
				break; // no more elements, so expecting a ')'
			}
			current = stream.read();
		}
		
		if (current != END_OF_MAP) {
			unexpected(END_OF_MAP);
		}
		
		return w.done();
	}

	private IValue readTuple(Type expected) throws IOException {
		ArrayList<IValue> arr = new ArrayList<IValue>();
		readFixed(expected, END_OF_TUPLE, arr);
		IValue[] result = new IValue[arr.size()];
		return factory.tuple(arr.toArray(result));
	}
	
	private IValue readSet(Type expected) throws FactTypeUseException, IOException {
		Type elemType = expected.isSetType() ? expected.getElementType() : types.valueType();
		return readContainer(elemType, expected.isSetType() ? factory.setWriter(elemType) : factory.setWriter(), END_OF_SET);
	}
		
	private IValue readList(Type expected) throws FactTypeUseException, IOException {
		Type elemType = expected.isListType() ? expected.getElementType() : types.valueType();
		return readList(elemType, expected.isListType() ? factory.listWriter(expected.getElementType()) : factory.listWriter(), END_OF_LIST);
	}

	private IValue readNumber(Type expected) throws IOException {
		StringBuilder builder = new StringBuilder();
	
		while (Character.isDigit(current) 
				|| current == DOUBLE_DOT) {
			builder.append((char) current);
			current = stream.read();
		}
		
		String val = builder.toString();
		
		try {
			return factory.integer(val);
		}
		catch (NumberFormatException e) {
			// could happen
		}
		
		try {
			return factory.real(val);
		}
		catch (NumberFormatException e) {
			// could happen
		}
		
		unexpected(current);
		return null;
	}

	private IValue readNode(Type expected) throws IOException {
		String id = readIdentifier();
		
		if (id.equals("true")) {
			return factory.bool(true);
		}
		else if (id.equals("false")) {
			return factory.bool(false);
		}
		else {
			if (current == START_OF_ARGUMENTS) {
				ArrayList<IValue> arr = new ArrayList<IValue>();
				Type args = expected;
				
				if (expected.isAbstractDataType()) {
					Set<Type> alternatives = store.lookupConstructor(expected, id);
					if (alternatives.size() > 1) {
						throw new OverloadingNotSupportedException(expected, id);
					}
					else if (alternatives.size() == 0) {
						args = types.valueType(); 
					}
					else {
						args = alternatives.iterator().next().getFieldTypes();
					}
				}
				readFixed(args, END_OF_ARGUMENTS, arr);
				IValue[] result = new IValue[arr.size()];
				result = arr.toArray(result);
				return expected.make(factory, store, id, result);
			}
			
			Type args = types.tupleType(new Type[0]);
			
			Type cons;
			if (expected.isAbstractDataType()) {
				cons = store.lookupConstructor(expected, id, args);
			}
			else {
				cons = store.lookupFirstConstructor(id, args);
			}
			
			if (cons != null) {
				return cons.make(factory);
			}
			
			return factory.node(id);
		}
	}

	/**
	 * Read in a single character from the input stream and append it to the
	 * given buffer only if it is numeric.
	 *  
	 * @param buf	The buffer to which the read character should be appended
	 * 
	 * @return		True if the input character was numeric [0-9] and was appended,
	 * 				false otherwise.
	 * 
	 * @throws IOException	when an error is encountered reading the input stream
	 */
	private boolean readAndAppendIfNumeric(StringBuilder buf) throws IOException {
		current = stream.read();
		if (Character.isDigit(current)) {
			buf.append((char)current);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Read in a date value, given as a string of the form NNNN-NN-NN, where each 
	 * N is a digit [0-9]. The groups are the year, month, and day of month.
	 * 
	 * @return A string of the form NNNNNNNN; i.e., the read string with all the
	 * 		   '-' characters removed
	 * 
	 * @throws IOException	when an error is encountered reading the input stream
	 * @throws FactParseError	when the correct characters are not found while lexing
	 *                          the date
	 */
	private String readDate(char firstChar) throws IOException, FactParseError {
		StringBuilder buf = new StringBuilder();
		buf.append(firstChar);
		
		// The first four characters should be the year
		for (int i = 0; i < 3; ++i) {
			boolean res = readAndAppendIfNumeric(buf);
			if (!res) {
				throw new FactParseError("Error reading date, expected digit, found: " + current, stream.offset);
			}
		}
		
		// The next character should be a '-'
		current = stream.read();		
		if ('-' != current) {
			throw new FactParseError("Error reading date, expected '-', found: " + current, stream.offset);
		}
		
		// The next two characters should be the month
		for (int i = 0; i < 2; ++i) {
			boolean res = readAndAppendIfNumeric(buf);
			if (!res) {
				throw new FactParseError("Error reading date, expected digit, found: " + current, stream.offset);
			}
		}
			
		// The next character should be a '-'
		current = stream.read();		
		if ('-' != current) {
			throw new FactParseError("Error reading date, expected '-', found: " + current, stream.offset);
		}
		
		// The next two characters should be the day
		for (int i = 0; i < 2; ++i) {
			boolean res = readAndAppendIfNumeric(buf);
			if (!res) {
				throw new FactParseError("Error reading date, expected digit, found: " + current, stream.offset);
			}
		}
		
		return buf.toString();
	}
	
	/**
	 * Read in a time value, given as a string of the form NN:NN:NN.NNN[+-]NN:NN,
	 * where each N is a digit [0-9]. The groups are the hour, minute, second,
	 * millisecond, timezone hour offset, and timezone minute offset.
	 *  
	 * @return A string of the form NNNNNNNNN[+-]NNNN, i.e., the read string
	 *         with all the colons removed.
	 *         
	 * @throws IOException	when an error is encountered reading the input stream
	 * @throws FactParseError	when the correct characters are not found while lexing
	 *                          the time
	 */
	private String readTime() throws IOException, FactParseError {
		StringBuilder buf = new StringBuilder();

		// The first two characters should be the hour
		for (int i = 0; i < 2; ++i) {
			boolean res = readAndAppendIfNumeric(buf);
			if (!res) {
				throw new FactParseError("Error reading time, expected digit, found: " + current, stream.offset);
			}
		}
		
		// The next character should be a ':'
		current = stream.read();		
		if (':' != current) {
			throw new FactParseError("Error reading time, expected ':', found: " + current, stream.offset);
		}
		
		// The next two characters should be the minute
		for (int i = 0; i < 2; ++i) {
			boolean res = readAndAppendIfNumeric(buf);
			if (!res) {
				throw new FactParseError("Error reading time, expected digit, found: " + current, stream.offset);
			}
		}
		
		// The next character should be a ':'
		current = stream.read();		
		if (':' != current) {
			throw new FactParseError("Error reading time, expected ':', found: " + current, stream.offset);
		}
		
		// The next two characters should be the second
		for (int i = 0; i < 2; ++i) {
			boolean res = readAndAppendIfNumeric(buf);
			if (!res) {
				throw new FactParseError("Error reading time, expected digit, found: " + current, stream.offset);
			}
		}
		
		// The next character should be a '.'
		current = stream.read();		
		if ('.' != current) {
			throw new FactParseError("Error reading time, expected '.', found: " + current, stream.offset);
		}
		
		// The next three characters should be the millisecond
		for (int i = 0; i < 3; ++i) {
			boolean res = readAndAppendIfNumeric(buf);
			if (!res) {
				throw new FactParseError("Error reading time, expected digit, found: " + current, stream.offset);
			}
		}
		
		// The next character should be '+' or '-'
		current = stream.read();		
		if (! ('+' == current || '-' == current)) {
			throw new FactParseError("Error reading time, expected '+' or '-', found: " + current, stream.offset);
		}
		buf.append((char)current);
		
		// The next two characters should be the hour offset
		for (int i = 0; i < 2; ++i) {
			boolean res = readAndAppendIfNumeric(buf);
			if (!res) {
				throw new FactParseError("Error reading time, expected digit, found: " + current, stream.offset);
			}
		}
		
		// The next two characters should be the minute offset
		for (int i = 0; i < 2; ++i) {
			boolean res = readAndAppendIfNumeric(buf);
			if (!res) {
				throw new FactParseError("Error reading time, expected digit, found: " + current, stream.offset);
			}
		}
		
		return buf.toString();
	}

	private IValue readDateTime(Type expected) throws IOException, FactParseError {
		String formatString = "";
		String toParse = "";		
		boolean isDate = false; 
		boolean isTime = false; 
		
		// Retrieve the string to parse and pick the correct format string,
		// based on whether we are reading a time, a date, or a datetime.
		current = stream.read();
		
		if ('T' == current || 't' == current) {
			toParse = readTime();
			formatString = "HHmmssSSSZZZZZ";
			isTime = true;
			current = stream.read(); // advance to next character for parsing
		} else {
			toParse = readDate((char)current);
			current = stream.read();
			if ('T' == current || 't' == current) {
				toParse = toParse + readTime();
				formatString = "yyyyMMddHHmmssSSSZZZZZ";
				current = stream.read(); // advance to next character for parsing
			} else {
				formatString = "yyyyMMdd";
				isDate = true;
				// no need to advance here, already did when checked for T
			}
		}
			
		SimpleDateFormat df = new SimpleDateFormat(formatString);
		
		try {
			Calendar cal = Calendar.getInstance();
			Date dt = df.parse(toParse);
			cal.setTime(dt);
			
			if (isDate) {
				return factory.date(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH));				
			} else { 
				int hourOffset = cal.get(Calendar.ZONE_OFFSET) / 3600000;
				int minuteOffset = (cal.get(Calendar.ZONE_OFFSET) / 60000)%60;
				if (isTime) {
					return factory.time(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), 
							cal.get(Calendar.MILLISECOND), hourOffset, minuteOffset);
				} else {
					return factory.datetime(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH),
							cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND), 
							cal.get(Calendar.MILLISECOND), hourOffset, minuteOffset);
				}
			}
		} catch (ParseException pe) {
			throw new FactParseError("Unable to parse datatime string " + toParse + " using format string " + formatString, stream.offset, pe);
		}		
	}

	private String readIdentifier() throws IOException {
		StringBuilder builder = new StringBuilder();
		boolean escaped = (current == '\\');

		if (escaped) {
			current = stream.read();
		}
		
		while (Character.isJavaIdentifierStart(current) 
				|| Character.isJavaIdentifierPart(current)
				|| (escaped && current == '-')) {
			builder.append((char) current);
			current = stream.read();
		}
		
		return builder.toString();
	}
	
	private IValue readString(Type expected) throws IOException {
		StringBuilder builder = new StringBuilder();
		current = stream.read();
		
		while (current != END_OF_STRING) {
			builder.append((char) current);
			current = stream.read();
			
			if (current == '\\') {
				current = stream.read();
				switch (current) {
				case 'n':
					builder.append('\n');
					break;
				case 't':
					builder.append('\t');
					break;
				case 'r':
					builder.append('\r');
					break;
				case '\\':
					builder.append('\\');
				}
				current = stream.read();
			}
		}
		
		String str = builder.toString();
		current = stream.read();
		
		
		if (current == '(') {
			ArrayList<IValue> arr = new ArrayList<IValue>();
			readFixed(expected, ')', arr);
			IValue[] result = new IValue[arr.size()];
			result = arr.toArray(result);
			
			return factory.node(str, result);
		}
		
		return factory.string(str);
	}

	private IValue readAnnos(Type expected, INode result) throws IOException {
		current = stream.read();
		
		while (current != ']') {
			checkAndRead('@');
			String key = readIdentifier();
			checkAndRead('=');
			
			Type annoType = getAnnoType(expected, key);
			IValue value = readValue(annoType);
	
			result = result.setAnnotation(key, value);
			current = stream.read();
			if (current != ',') {
				break; // no more elements, so expecting a ']'
			}
			current = stream.read();
		}
		
		return result;
	}

	private Type getAnnoType(Type expected, String key) {
		Type annoType;
		if (expected.isAbstractDataType() || expected.isConstructorType()) {
			if (expected.declaresAnnotation(store, key)) {
				annoType = store.getAnnotationType(expected, key);
			}
			else {
				annoType = types.valueType();
			}
		}
		else {
			annoType = types.valueType();
		}
		return annoType;
	}

	private void readFixed(Type expected, char end, List<IValue> arr) throws IOException {
		current = stream.read();
		
	   for (int i = 0; current != end; i++) {
		   Type exp = expected.isTupleType() ? expected.getFieldType(i) : types.valueType();
		   arr.add(readValue(exp));
		   
		   if (current != ',' || current == end) {
			   break; // no more elements, so expecting a '>'
		   }
		   current = stream.read();
	   }
	
	   checkAndRead(end);
	}

	private IValue readContainer(Type elemType, IWriter w, char end) throws FactTypeUseException, IOException {
		current = stream.read();
		for (int i = 0; current != end; i++) {
			w.insert(readValue(elemType));
	
			if (current != ',' || current == end) {
				break; // no more elements, so expecting a '}'
			}
			current = stream.read();
		}
	
		checkAndRead(end);
	
		return w.done();
	}
	
	private IValue readList(Type elemType, IListWriter w, char end) throws FactTypeUseException, IOException {
		current = stream.read();
		for (int i = 0; current != end; i++) {
			w.append(readValue(elemType));
	
			if (current != ',' || current == end) {
				break; // no more elements, so expecting a '}'
			}
			current = stream.read();
		}
	
		checkAndRead(end);
	
		return w.done();
	}

	private void checkAndRead(char c) throws IOException {
		if (current != c) {
			unexpected(c);
		}
		current = stream.read();
	}

	private void unexpected(int c) {
		throw new FactParseError("Expected " + ((char) c) + " but got " + ((char) current), stream.getOffset());
	}

	private void unexpected() {
		throw new FactParseError("Unexpected " + ((char) current), stream.getOffset());
	}

	private class NoWhiteSpaceInputStream extends InputStream {
		private InputStream wrapped;
		int offset;
		int line;
		int column;
	
		public NoWhiteSpaceInputStream(InputStream wrapped) {
			this.wrapped = wrapped;
		}
		
		@Override
		public int read() throws IOException {
			int r = wrapped.read();
			offset++;
			column++;
			
			while (Character.isWhitespace(r)) {
				offset++;
				if (r == '\n') {
					line++;
					column = 0;
				}
				else {
					column++;
				}
				r = wrapped.read();
			}
	
			return r;
		}
		
		int getOffset() {
			return offset;
		}
	}
}