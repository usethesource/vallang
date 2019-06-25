package io.usethesource.vallang.type;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import io.usethesource.vallang.exceptions.TypeParseError;

public class TypeReader {
    private static final char TYPE_PARAMETER_TOKEN = '&';
    private static final char START_OF_ARGUMENTS = '[';
    private static final char END_OF_ARGUMENTS = ']';
    private static final char COMMA_SEPARATOR = ',';

    private TypeStore store;
    private NoWhiteSpaceReader stream;
    private TypeFactory types;
    private int current;

    public Type read(TypeStore store, Reader stream) throws IOException {
        this.store = store;
        this.stream = new NoWhiteSpaceReader(stream);
        this.types = TypeFactory.getInstance();

        current = this.stream.read();
        Type result = readType();
        if (current != -1 || this.stream.read() != -1) {
            unexpected();
        }
        return result;
    }

    private Type readType() throws IOException {
        if (current == TYPE_PARAMETER_TOKEN) {
            return readTypeParameter();
        }

        if (Character.isJavaIdentifierStart(current)) {
            String id = readIdentifier();

            switch (id) {
            case "int" : return types.integerType();
            case "real" : return types.realType();
            case "rat" : return types.rationalType();
            case "num" : return types.numberType();
            case "bool" : return types.boolType();
            case "node" : return types.nodeType();
            case "void" : return types.voidType();
            case "value" : return types.valueType();
            case "loc" : return types.sourceLocationType();
            case "str" : return types.stringType();
            case "datetime" : return types.dateTimeType();
            }

            if (current == START_OF_ARGUMENTS) {
                switch (id) {
                case "list" : return readComposite((t) -> types.listType(t.get(0)));
                case "set" : return readComposite((t) -> types.setType(t.get(0)));
                case "map" : return readComposite((t) -> types.mapType(t.get(0), t.get(1)));
                case "tuple" : return readComposite((t) -> types.tupleType(t.toArray(new Type[0])));
                case "rel" : return readComposite((t) -> types.relType(t.toArray(new Type[0])));
                case "lrel" : return readComposite((t) -> types.lrelType(t.toArray(new Type[0])));
                }

                Type adt = store.lookupAbstractDataType(id);
                if (adt != null) {
                    return readComposite((t) -> types.abstractDataType(store, id, t.toArray(new Type[0])));
                }

                throw new TypeParseError("undeclared type " + id, new NullPointerException()); 
            }
            else {
                Type adt = store.lookupAbstractDataType(id);
                if (adt != null) {
                    return adt;
                }

                throw new TypeParseError("undeclared type " + id, new NullPointerException()); 
            }
        }

        throw new TypeParseError("Unexpected " + ((char) current), stream.getOffset());
    }

    private Type readTypeParameter() throws IOException {
        String name = readIdentifier();

        if (current == '<') {
            checkAndRead(':');

            return types.parameterType(name, readType());
        }

        return types.parameterType(name);
    }

    private Type readComposite(Function<List<Type>,Type> wrap) throws IOException {
        ArrayList<Type> arr = new ArrayList<>();
        readFixed(END_OF_ARGUMENTS, arr);
        return wrap.apply(arr);
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

    private void readFixed(char end, List<Type> arr) throws IOException {
        current = stream.read();

        while (current != end) {
            arr.add(readType());

            if (current == end) {
                break; // no more elements, so expecting a 'end'
            }

            current = stream.read();
        }

        checkAndRead(end);
    }

    private void checkAndRead(char c) throws IOException {
        if (current != c) {
            unexpected(c);
        }
        current = stream.read();
    }

    private void unexpected(int c) {
        throw new TypeParseError("Expected " + ((char) c) + " but got " + ((char) current), stream.getOffset());
    }

    private void unexpected() {
        throw new TypeParseError("Unexpected " + ((char) current), stream.getOffset());
    }

    private static class NoWhiteSpaceReader extends Reader {
        private Reader wrapped;
        int offset;
        boolean inString = false;
        boolean escaping = false;

        public NoWhiteSpaceReader(Reader wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() throws IOException {
            int r = wrapped.read();
            offset++;

            if (!inString) {
                while (Character.isWhitespace(r)) {
                    offset++;
                    r = wrapped.read();
                }
            }

            if (!inString && r == '\"') {
                inString = true;
            }
            else if (inString) {
                if (escaping) {
                    // previous was escaping, so no interpretation of current char.
                    escaping = false;
                }
                else if (r == '\\') {
                    escaping = true;
                }
                else if (r == '"') {
                    // if we were not escaped, a double quote exits a string
                    inString = false;
                }
            }

            return r;
        }

        int getOffset() {
            return offset;
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
        }
    }
}
