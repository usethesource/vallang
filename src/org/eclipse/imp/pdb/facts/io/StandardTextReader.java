package org.eclipse.imp.pdb.facts.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.IWriter;
import org.eclipse.imp.pdb.facts.exceptions.FactParseError;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;

public class StandardTextReader extends AbstractReader {

	private static final char START_OF_RANGE = '?';
	private static final char START_OF_LOC = '!';
	private static final char START_OF_MAP = '(';
	private static final char START_OF_TUPLE = '<';
	private static final char START_OF_SET = '{';
	private static final char START_OF_LIST = '[';
	private static final char END_OF_TUPLE = '>';
	private static final char COMMA_SEPARATOR = ',';
	private static final char END_OF_MAP = ')';
	private static final char DOUBLE_DOT = '.';
	private static final char END_OF_SET = '}';
	private static final char END_OF_LIST = ']';
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
		else if (Character.isJavaIdentifierStart(current)
				|| current == '\\') {
			result = readNode(expected);
		}
		else {
			switch (current) {
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
			case START_OF_RANGE:
				result = readRange(expected);
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

	private IValue readRange(Type expected) throws IOException {
		int off = -1, len = -1, sl = -1, el = -1, sc = -1, ec = -1;
		
		current = stream.read();
		
		do {
			switch (current) {
			case 'o':
				current = stream.read();
				checkAndRead('f');
				checkAndRead('f');
				checkAndRead('=');
				off = ((IInteger) readNumber(types.integerType())).getValue();
				break;
			case 'l':
				current = stream.read();
				checkAndRead('e');
				checkAndRead('n');
				checkAndRead('=');
				len = ((IInteger) readNumber(types.integerType())).getValue();
				break;
			case 's':
				current = stream.read();
				checkAndRead('t');
				checkAndRead('a');
				checkAndRead('r');
				checkAndRead('t');
				checkAndRead('=');
				sl = ((IInteger) readNumber(types.integerType())).getValue();
				checkAndRead(',');
				sc = ((IInteger) readNumber(types.integerType())).getValue();
				break;
			case 'e':
				current = stream.read();
				checkAndRead('n');
				checkAndRead('d');
				checkAndRead('=');
				el = ((IInteger) readNumber(types.integerType())).getValue();
				checkAndRead(',');
				ec = ((IInteger) readNumber(types.integerType())).getValue();
				break;
			default:
				unexpected();
			}
			
			if (current == '&') {
				current = stream.read();
				continue;
			}
			else {
				break;
			}
		} while (true);
		
		if (len == -1 || off == -1 || sl == -1 || sc == -1 || el == -1 || ec == -1) {
		  throw new FactParseError("Incomplete range", new IOException());	
		}
		
		return factory.sourceRange(off, len, sl, el, sc, ec);
	}

	private IValue readLocation(Type expected) throws IOException {
		current = stream.read();
		String url = readIdentifier();
		
		if (!url.equals("file")) {
			throw new FactParseError("No other than file locations are supported", new IOException());
		}
		
		checkAndRead(':');
		checkAndRead('/');
		checkAndRead('/');
		String fileName = readFileName();
		
		ISourceRange range;
		if (current == '?') {
			range = (ISourceRange) readRange(types.sourceRangeType());
		}
		else {
			range = factory.sourceRange(0, 0, 1, 1, 0, 0);
		}
		
		return factory.sourceLocation(fileName, range);
	}

	private String readFileName() throws IOException {
		StringBuilder builder = new StringBuilder();
		
		// TODO support for URL path syntax
		while (Character.isLetterOrDigit(current) ||
				current == '/' || current == '_' || current == '-') {
			builder.append((char) current);
			current = stream.read();
		}
		
		return builder.toString();
				
	}

	private IValue readMap(Type expected) throws IOException {
		Type keyType = expected.isMapType() ? expected.getKeyType() : types.valueType();
		Type valueType = expected.isMapType() ? expected.getValueType() : types.valueType();
		IMapWriter w = factory.mapWriter(keyType, valueType);

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
		return readContainer(expected, factory.setWriter(elemType), END_OF_SET);
	}
		
	private IValue readList(Type expected) throws FactTypeUseException, IOException {
		Type elemType = expected.isListType() ? expected.getElementType() : types.valueType();
		return readContainer(expected, factory.listWriter(elemType), END_OF_LIST);
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
			return factory.integer(Integer.parseInt(val));
		}
		catch (NumberFormatException e) {
			// could happen
		}
		
		try {
			return factory.dubble(Double.parseDouble(val));
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
			if (current == '(') {
				ArrayList<IValue> arr = new ArrayList<IValue>();
				readFixed(expected, ')', arr);
				IValue[] result = new IValue[arr.size()];
				result = arr.toArray(result);
				
				if (expected.isConstructorType()) {
					return expected.make(factory, result);
				}
				else if (expected.isAbstractDataType()) {
					Type[] children = new Type[arr.size()];
					for (int i = 0; i < arr.size(); i++) {
						children[i] = arr.get(i).getType();
					}
					Type args = types.tupleType(children);
					Type cons = store.lookupConstructor(expected, id, args);
					
					if (cons != null) {
						return cons.make(factory, result);
					}
				}
				
				return factory.node(id, result);
			}
			else {
				Type args = types.tupleType(new Type[0]);
				Type cons = store.lookupConstructor(expected, id, args);
				
				if (cons != null) {
					return cons.make(factory);
				}
				else {
					return factory.node(id);
				}
			}
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
		   Type exp = expected.isTupleType() || expected.isConstructorType() ? expected.getFieldType(i) : types.valueType();
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
		
		int getLine() {
			return line;
		}
		
		int getColumn() {
			return column;
		}
		
		int getOffset() {
			return offset;
		}
	}
}