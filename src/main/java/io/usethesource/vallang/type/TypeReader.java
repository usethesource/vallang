package io.usethesource.vallang.type;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import io.usethesource.vallang.exceptions.TypeParseError;

public class TypeReader {
    private static final char TYPE_PARAMETER_TOKEN = '&';
    private static final char START_OF_ARGUMENTS = '[';
    private static final char END_OF_ARGUMENTS = ']';
    private static final char COMMA_SEPARATOR = ',';
    private static final TypeFactory types = TypeFactory.getInstance();

    private int current;

    public Type read(TypeStore store, Reader reader) throws IOException {
        try (NoWhiteSpaceReader stream = new NoWhiteSpaceReader(reader)) {
            current = stream.read();
            Type result = readType(store, stream);
            if (current != -1 || stream.read() != -1) {
                unexpected(stream);
            }
            return result;
        }
    }

    private Type readType(TypeStore store, NoWhiteSpaceReader stream) throws IOException {
        if (current == TYPE_PARAMETER_TOKEN) {
            checkAndRead(stream, TYPE_PARAMETER_TOKEN);
            return readTypeParameter(store, stream);
        }

        if (Character.isJavaIdentifierStart(current)) {
            String id = readIdentifier(store, stream);

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
                    case "list" : return readComposite(store, stream, (t) -> types.listType(t.get(0)));
                    case "set" : return readComposite(store, stream, (t) -> types.setType(t.get(0)));
                    case "map" : return readComposite(store, stream, (t, l) -> l.isEmpty() ? types.mapType(t.get(0), t.get(1)) : types.mapType(t.get(0), l.get(0), t.get(1), l.get(1)));
                    case "tuple" : return readComposite(store, stream, (t, l) -> l.isEmpty() ? types.tupleType(t.toArray(new Type[0])) : types.tupleType(interleave(t, l)));
                    case "rel" : return readComposite(store, stream, (t, l) -> l.isEmpty() ? types.relType(t.toArray(new Type[0])): types.relType(interleave(t, l)));
                    case "lrel" : return readComposite(store, stream, (t, l) -> l.isEmpty() ? types.lrelType(t.toArray(new Type[0])) : types.lrelType(interleave(t, l)));
                }

                Type adt = store.lookupAbstractDataType(id);
                if (adt != null) {
                    return readComposite(store, stream, (t) -> types.abstractDataType(store, id, t.toArray(new Type[0])));
                }

                throw new TypeParseError("undeclared type " + id, new IllegalArgumentException());
            }
            else {
                Type adt = store.lookupAbstractDataType(id);
                if (adt != null) {
                    return adt;
                }

                throw new TypeParseError("undeclared type " + id, new IllegalArgumentException());
            }
        }

        throw new TypeParseError("Unexpected " + ((char) current), stream.getOffset());
    }

    private static Object[] interleave(List<Type> t, List<String> l) {
        assert t.size() == l.size();
        var result = new Object[t.size() + l.size()];
        for (int i = 0; i < t.size(); i++) {
            result[i * 2] = t.get(i);
            result[(i * 2) + 1] = l.get(i);
        }
        return result;
    }

    private Type readTypeParameter(TypeStore store, NoWhiteSpaceReader stream) throws IOException {
        String name = readIdentifier(store, stream);

        if (current == '<') {
            checkAndRead(stream, '<');
            checkAndRead(stream, ':');

            return types.parameterType(name, readType(store, stream));
        }

        return types.parameterType(name);
    }

    private Type readComposite(TypeStore store, NoWhiteSpaceReader stream, Function<List<Type>,Type> wrap) throws IOException {
        ArrayList<Type> arr = new ArrayList<>();
        readFixed(store, stream, END_OF_ARGUMENTS, arr, null);
        return wrap.apply(arr);
    }

    private Type readComposite(TypeStore store, NoWhiteSpaceReader stream, BiFunction<List<Type>, List<String>,Type> wrap) throws IOException {
        ArrayList<Type> arr = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        readFixed(store, stream, END_OF_ARGUMENTS, arr, labels);
        return wrap.apply(arr, labels);
    }

    private String readIdentifier(TypeStore store, NoWhiteSpaceReader stream) throws IOException {
        // identifiers should not cross whitespace
        // so we first swallow whitespace
        if (Character.isWhitespace(current)) {
            current = stream.read();
        }
        StringBuilder builder = new StringBuilder();
        boolean escaped = (current == '\\');

        // from now on we read raw, don't eat whitespace
        if (escaped) {
            current = stream.readRaw();
        }

        while (Character.isJavaIdentifierStart(current)
                || Character.isJavaIdentifierPart(current)
                || (escaped && current == '-')) {
            builder.append((char) current);
            current = stream.readRaw();
        }

        // if we're at the end, we swallow whitespace again
        if (Character.isWhitespace(current)) {
            current = stream.read();
        }

        return builder.toString();
    }

    private void readFixed(TypeStore store, NoWhiteSpaceReader stream, char end, List<Type> arr, @Nullable List<String> labels) throws IOException {
        current = stream.read();

        while (current != end) {
            arr.add(readType(store, stream));

            if (current == end) {
                break; // no more elements, so expecting a 'end'
            }

            if (labels != null && Character.isJavaIdentifierStart(current)) {
                labels.add(readIdentifier(store, stream));
            }

            if (current == end) {
                break; // no more elements, so expecting a 'end'
            }

            checkAndRead(stream, COMMA_SEPARATOR);
        }

        checkAndRead(stream, end);
    }

    private void checkAndRead(NoWhiteSpaceReader stream, char c) throws IOException {
        if (current != c) {
            unexpected(stream, c);
        }
        current = stream.read();
    }

    private void unexpected(NoWhiteSpaceReader stream, int c) {
        throw new TypeParseError("Expected " + ((char) c) + " but got " + ((char) current), stream.getOffset());
    }

    private void unexpected(NoWhiteSpaceReader stream) {
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

        // sometimes you want to read the raw value, even if it's whitespace
        public int readRaw() throws IOException {
            int r = wrapped.read();
            offset++;
            return r;
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
