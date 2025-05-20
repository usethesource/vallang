/**
 * Copyright (c) 2016, Davy Landman, Paul Klint, Centrum Wiskunde & Informatica (CWI)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.usethesource.vallang.io.binary.message;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import io.usethesource.capsule.Map;
import io.usethesource.capsule.Map.Immutable;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.io.binary.stream.IValueInputStream;
import io.usethesource.vallang.io.binary.util.TrackLastRead;
import io.usethesource.vallang.io.binary.util.WindowCacheFactory;
import io.usethesource.vallang.io.binary.wire.IWireInputStream;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * An utility class for the {@link IValueInputStream}. Only directly use methods in this class if you have nested IValues in an existing {@link IWireInputStream}.
 */
public class IValueReader {
    private static final TypeFactory tf = TypeFactory.getInstance();
    private static final Type VOID_TYPE = tf.voidType();

    /**
     * Read a value from the wire reader. <br/>
     * <br/>
     * In most cases you want to use the {@linkplain IValueInputStream}!
     */
    public static IValue readValue(IWireInputStream reader, IValueFactory vf, Supplier<TypeStore> typeStoreSupplier) throws IOException {
        int typeWindowSize = 0;
        int valueWindowSize = 0;
        int uriWindowSize = 0;
        if (reader.next() != IWireInputStream.MESSAGE_START || reader.message() != IValueIDs.Header.ID) {
            throw new IOException("Missing header at start of stream");
        }
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch (reader.field()) {
                case IValueIDs.Header.VALUE_WINDOW:
                    valueWindowSize = reader.getInteger();
                    break;
                case IValueIDs.Header.TYPE_WINDOW:
                    typeWindowSize = reader.getInteger();
                    break;
                case IValueIDs.Header.SOURCE_LOCATION_WINDOW:
                    uriWindowSize = reader.getInteger();
                    break;
                case IValueIDs.Header.VALUE: {
                    IValueReader valueReader = new IValueReader(vf, typeStoreSupplier, typeWindowSize, valueWindowSize, uriWindowSize);
                    try {
                        IValue result = valueReader.readValue(reader);
                        reader.skipMessage();
                        return result;
                    } finally {
                        valueReader.done();
                    }
                }
                default:
                    reader.skipNestedField();
                    break;
            }
        }
        throw new IOException("Missing Value in the stream");
    }

    /**
     * Read a type from the wire reader.
     */
    public static Type readType(IWireInputStream reader, IValueFactory vf, Supplier<TypeStore> typeStoreSupplier) throws IOException {
        int typeWindowSize = 0;
        int valueWindowSize = 0;
        int uriWindowSize = 0;
        if (reader.next() != IWireInputStream.MESSAGE_START || reader.message() != IValueIDs.Header.ID) {
            throw new IOException("Missing header at start of stream");
        }
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch (reader.field()) {
                case IValueIDs.Header.VALUE_WINDOW:
                    valueWindowSize = reader.getInteger();
                    break;
                case IValueIDs.Header.TYPE_WINDOW:
                    typeWindowSize = reader.getInteger();
                    break;
                case IValueIDs.Header.SOURCE_LOCATION_WINDOW:
                    uriWindowSize = reader.getInteger();
                    break;
                case IValueIDs.Header.TYPE: {
                    IValueReader valueReader = new IValueReader(vf, typeStoreSupplier, typeWindowSize, valueWindowSize, uriWindowSize);
                    try {
                        Type result = valueReader.readType(reader);
                        reader.skipMessage();
                        return result;
                    } finally {
                        valueReader.done();
                    }
                }
                default:
                    reader.skipNestedField();
                    break;
            }
        }
        throw new IOException("Missing Type in the stream");
    }

    private IValueReader(IValueFactory vf, Supplier<TypeStore> typeStoreSupplier, int typeWindowSize, int valueWindowSize, int uriWindowSize) {
        WindowCacheFactory windowFactory = WindowCacheFactory.getInstance();
        typeWindow = windowFactory.getTrackLastRead(typeWindowSize);
        valueWindow = windowFactory.getTrackLastRead(valueWindowSize);
        uriWindow = windowFactory.getTrackLastRead(uriWindowSize);

        this.typeStoreSupplier = typeStoreSupplier;

        this.vf = vf;
        this.store = typeStoreSupplier.get();
    }

    private void done() {
        WindowCacheFactory windowFactory = WindowCacheFactory.getInstance();
        windowFactory.returnTrackLastRead(typeWindow);
        windowFactory.returnTrackLastRead(valueWindow);
        windowFactory.returnTrackLastRead(uriWindow);
    }

    private final Supplier<TypeStore> typeStoreSupplier;

    private final IValueFactory vf;
    private final TypeStore store;

    private final TrackLastRead<Type> typeWindow;
    private final TrackLastRead<IValue> valueWindow;
    private final TrackLastRead<ISourceLocation> uriWindow;

    @SuppressWarnings("deprecation")
    private Type readType(final IWireInputStream reader) throws IOException{
        reader.next();
        switch (reader.message()) {
            case IValueIDs.BoolType.ID:
                reader.skipMessage(); // forward to the end
                return tf.boolType();

            case IValueIDs.DateTimeType.ID:
                reader.skipMessage();
                return tf.dateTimeType();

            case IValueIDs.IntegerType.ID:
                reader.skipMessage();
                return tf.integerType();

            case IValueIDs.NodeType.ID:
                reader.skipMessage();
                return tf.nodeType();

            case IValueIDs.NumberType.ID:
                reader.skipMessage();
                return tf.numberType();

            case IValueIDs.RationalType.ID:
                reader.skipMessage();
                return tf.rationalType();

            case IValueIDs.RealType.ID:
                reader.skipMessage();
                return tf.realType();

            case IValueIDs.SourceLocationType.ID:
                reader.skipMessage();
                return tf.sourceLocationType();

            case IValueIDs.StringType.ID:
                reader.skipMessage();
                return tf.stringType();

            case IValueIDs.ValueType.ID:
                reader.skipMessage();
                return tf.valueType();

            case IValueIDs.VoidType.ID:
                reader.skipMessage();
                return tf.voidType();

                // Composite types

            case IValueIDs.ADTType.ID: {
                String name = "";
                boolean backReference = false;
                Type typeParam = VOID_TYPE;

                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch(reader.field()){
                        case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                            backReference = true;
                            break;
                        case IValueIDs.ADTType.NAME:
                            name = reader.getString();
                            break;
                        case IValueIDs.ADTType.TYPE_PARAMS:
                            typeParam = readType(reader);
                            break;
                        default:
                            reader.skipNestedField();
                            break;
                    }
                }
                assert !name.isEmpty();

                return returnAndStore(backReference, typeWindow, tf.abstractDataTypeFromTuple(store, name, typeParam));
            }

            case IValueIDs.AliasType.ID:   {
                String name = "";
                boolean backReference = false;
                Type typeParameters = VOID_TYPE;
                Type aliasedType = VOID_TYPE;

                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch(reader.field()){
                        case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                            backReference = true;
                            break;
                        case IValueIDs.AliasType.NAME:
                            name = reader.getString();
                            break;
                        case IValueIDs.AliasType.ALIASED:
                            aliasedType = readType(reader);
                            break;
                        case IValueIDs.AliasType.TYPE_PARAMS:
                            typeParameters = readType(reader);
                            break;
                        default:
                            reader.skipNestedField();
                            break;
                    }
                }

                assert !name.isEmpty() && aliasedType != VOID_TYPE;


                return returnAndStore(backReference, typeWindow, tf.aliasTypeFromTuple(store, name, aliasedType, typeParameters));
            }

            case IValueIDs.ConstructorType.ID:     {
                String name = "";
                boolean backReference = false;
                Type fieldTypes = VOID_TYPE;
                Type adtType = VOID_TYPE;
                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch(reader.field()){
                        case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                            backReference = true;
                            break;
                        case IValueIDs.ConstructorType.NAME:
                            name = reader.getString();
                            break;
                        case IValueIDs.ConstructorType.ADT:
                            adtType = readType(reader);
                            break;
                        case IValueIDs.ConstructorType.FIELD_TYPES:
                            fieldTypes = readType(reader);
                            break;
                    }
                }

                assert !name.isEmpty() && fieldTypes != VOID_TYPE && adtType != VOID_TYPE;

                return returnAndStore(backReference, typeWindow, tf.constructorFromTuple(store, adtType, name, fieldTypes));
            }

            // External
            case IValueIDs.ExternalType.ID: {

                boolean backReference = false;
                @MonotonicNonNull IConstructor symbol = null;
                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch(reader.field()){
                        case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                            backReference = true;
                            break;
                        case IValueIDs.ExternalType.SYMBOL:
                            symbol = (IConstructor) readValue(reader);
                            break;
                    }
                }

                if (symbol == null) {
                    throw new IOException("ExternalType is missing the SYMBOL field");
                }

                /**
                 * TODO previous code allocated a new {@link TypeStore} instance from the
                 * {@link RascalValueFactory}. Is creating a new instance necessary here?
                 */
                return returnAndStore(backReference, typeWindow, tf.fromSymbol(symbol, typeStoreSupplier.get(), p -> Collections.emptySet()));
            }

            case IValueIDs.ListType.ID:    {
                boolean backReference = false;
                Type elemType = VOID_TYPE;
                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch(reader.field()){
                        case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                            backReference = true;
                            break;
                        case IValueIDs.ListType.ELEMENT_TYPE:
                            elemType = readType(reader);
                            break;
                    }
                }
                return returnAndStore(backReference, typeWindow, tf.listType(elemType));
            }

            case IValueIDs.MapType.ID: {
                boolean backReference = false;
                Type valType = VOID_TYPE;
                Type keyType = VOID_TYPE;
                String valName = null;
                String keyName = null;
                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch(reader.field()){
                        case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                            backReference = true;
                            break;
                        case IValueIDs.MapType.KEY_TYPE:
                            keyType = readType(reader);
                            break;
                        case IValueIDs.MapType.KEY_NAME:
                            keyName = reader.getString();
                            break;
                        case IValueIDs.MapType.VALUE_TYPE:
                            valType = readType(reader);
                            break;
                        case IValueIDs.MapType.VALUE_NAME:
                            valName = reader.getString();
                            break;
                    }
                }
                Type tp;
                if (valName != null && keyName != null) {
                    tp = tf.mapType(keyType, keyName, valType, valName);
                }
                else {
                    tp = tf.mapType(keyType, valType);
                }
                return returnAndStore(backReference, typeWindow, tp);
            }

            case IValueIDs.ParameterType.ID:   {
                String name = "";
                boolean backReference = false;
                Type bound = VOID_TYPE;

                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch (reader.field()){
                        case IValueIDs.ParameterType.NAME:
                            name = reader.getString();
                            break;
                        case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                            backReference = true;
                            break;
                        case IValueIDs.ParameterType.BOUND:
                            bound = readType(reader);
                            break;

                    }
                }
                assert !name.isEmpty();

                return returnAndStore(backReference, typeWindow, tf.parameterType(name, bound));
            }

            case IValueIDs.SetType.ID: {
                boolean backReference = false;
                Type elemType = VOID_TYPE;
                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch(reader.field()){
                        case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                            backReference = true;
                            break;
                        case IValueIDs.SetType.ELEMENT_TYPE:
                            elemType = readType(reader);
                            break;
                    }
                }
                return returnAndStore(backReference, typeWindow, tf.setType(elemType));
            }

            case IValueIDs.TupleType.ID: {
                boolean backReference = false;
                String [] fieldNames = new String[0];
                Type[] elemTypes = new Type[0];

                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch (reader.field()){
                        case IValueIDs.TupleType.NAMES:
                            fieldNames = reader.getStrings();
                            break;
                        case IValueIDs.TupleType.TYPES:
                            elemTypes = new Type[reader.getRepeatedLength()];
                            for (int i = 0; i < elemTypes.length; i++) {
                                elemTypes[i] = readType(reader);
                            }
                            break;
                        case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                            backReference = true;
                            break;
                    }
                }

                if (fieldNames.length != 0){
                    assert fieldNames.length == elemTypes.length;
                    return returnAndStore(backReference, typeWindow, tf.tupleType(elemTypes, fieldNames));
                } else {
                    return returnAndStore(backReference, typeWindow, tf.tupleType(elemTypes));
                }
            }

            case IValueIDs.PreviousType.ID: {
                int n = -1;
                while (reader.next() != IWireInputStream.MESSAGE_END) {
                    switch (reader.field()){
                        case IValueIDs.PreviousType.HOW_LONG_AGO:
                            n = reader.getInteger();
                            break;
                        default:
                            reader.skipNestedField();
                            break;
                    }
                }
                if (n == -1) {
                    throw new IOException("Missing HOW_LONG_AGO field in PreviousType");
                }

                return typeWindow.lookBack(n);
            }
            default:
                throw new IOException("Unexpected new message: " + reader.message());
        }
    }

    private static <T extends @NonNull Object> T returnAndStore(boolean backReferenced, TrackLastRead<T> window, T value) {
        if (backReferenced) {
            window.read(value);
        }
        return value;
    }


    private IValue readValue(final IWireInputStream reader) throws IOException{
        reader.next();
        assert reader.current() == IWireInputStream.MESSAGE_START;
        switch (reader.message()) {
            case IValueIDs.BoolValue.ID: return readBoolean(reader);
            case IValueIDs.ConstructorValue.ID:    return readConstructor(reader);
            case IValueIDs.DateTimeValue.ID: return readDateTime(reader);
            case IValueIDs.IntegerValue.ID: return readInteger(reader);
            case IValueIDs.ListValue.ID: return readList(reader);
            case IValueIDs.SourceLocationValue.ID: return readSourceLocation(reader);
            case IValueIDs.MapValue.ID: return readMap(reader);
            case IValueIDs.NodeValue.ID: return readNode(reader);
            case IValueIDs.RationalValue.ID: return readRational(reader);
            case IValueIDs.RealValue.ID: return readReal(reader);
            case IValueIDs.SetValue.ID: return readSet(reader);
            case IValueIDs.StringValue.ID: return readString(reader);
            case IValueIDs.TupleValue.ID: return readTuple(reader);
            case IValueIDs.PreviousValue.ID: return readPreviousValue(reader);
            default:
                throw new IllegalArgumentException("readValue: " + reader.message());
        }
    }

    private IValue readPreviousValue(final IWireInputStream reader) throws IOException {
        int n = -1;
        while(reader.next() != IWireInputStream.MESSAGE_END){
            if(reader.field() == IValueIDs.PreviousValue.HOW_FAR_BACK){
                n = reader.getInteger();
                reader.skipMessage();
                break;
            }
        }

        if (n == -1) {
            throw new IOException("Missing or incorrect HOW_FAR_BACK field");
        }

        return valueWindow.lookBack(n);
    }


    private IValue readTuple(final IWireInputStream reader) throws IOException {
        boolean backReference = false;
        IValue[] children = new IValue[0];
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()) {
                case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                    backReference = true;
                    break;
                case IValueIDs.TupleValue.CHILDREN:
                    int size = reader.getRepeatedLength();
                    children = new IValue[size];
                    switch (size) {
                        case 1:
                            children[0] = readValue(reader);
                            break;
                        case 2:
                            children[0] = readValue(reader);
                            children[1] = readValue(reader);
                            break;
                        default:
                            for (int i = 0; i < children.length; i++) {
                                children[i] = readValue(reader);
                            }
                            break;
                    }
                    break;
                default:
                    reader.skipNestedField();
                    break;
            }
        }
        return returnAndStore(backReference, valueWindow, vf.tuple(children));
    }

    private IValue readSet(final IWireInputStream reader) throws IOException {
        ISet result = vf.set();
        boolean backReference = false;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()) {
                case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                    backReference = true;
                    break;
                case IValueIDs.SetValue.ELEMENTS:
                    int size = reader.getRepeatedLength();
                    switch (size) {
                        case 0:
                            result = vf.set();
                            break;
                        case 1:
                            result = vf.set(readValue(reader));
                            break;
                        case 2:
                            result = vf.set(readValue(reader), readValue(reader));
                            break;
                        default:
                            ISetWriter writer = vf.setWriter();
                            for (int i = 0; i < size; i++) {
                                writer.insert(readValue(reader));
                            }
                            result = writer.done();
                            break;
                    }
                    break;
            }
        }
        return returnAndStore(backReference, valueWindow, result);
    }

    private IValue readList(final IWireInputStream reader) throws IOException {
        IList result = vf.list();
        boolean backReference = false;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()) {
                case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                    backReference = true;
                    break;
                case IValueIDs.ListValue.ELEMENTS:
                    int size = reader.getRepeatedLength();
                    switch (size) {
                        case 0:
                            result = vf.list();
                            break;
                        case 1:
                            result = vf.list(readValue(reader));
                            break;
                        case 2:
                            IValue first = readValue(reader);
                            IValue second = readValue(reader);
                            result = vf.list(first, second);
                            break;
                        default:
                            IListWriter writer = vf.listWriter();
                            for (int i = 0; i < size; i++) {
                                writer.append(readValue(reader));
                            }
                            result = writer.done();
                            break;
                    }
                    break;
            }
        }
        return returnAndStore(backReference, valueWindow, result);
    }

    private IValue readMap(final IWireInputStream reader) throws IOException {
        IMapWriter result = vf.mapWriter();
        boolean backReference = false;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()) {
                case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                    backReference = true;
                    break;
                case IValueIDs.MapValue.KV_PAIRS:
                    int size = reader.getRepeatedLength();
                    for (int i = 0; i < size; i += 2) {
                        IValue key = readValue(reader);
                        IValue value = readValue(reader);
                        result.put(key, value);;
                    }
                    break;
            }
        }
        return returnAndStore(backReference, valueWindow, result.done());
    }

    private IValue readNode(final IWireInputStream reader) throws IOException {
        String name = "";
        IValue[] children = new IValue[0];
        Map.Immutable<String, IValue> kwParams = Map.Immutable.of();
        Map.Immutable<String, IValue> annos = Map.Immutable.of();


        boolean backReference = false;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()){
                case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                    backReference = true;
                    break;
                case IValueIDs.NodeValue.NAME:
                    name = reader.getString();
                    break;
                case IValueIDs.NodeValue.PARAMS:
                    children = new IValue[reader.getRepeatedLength()];
                    for (int i = 0; i < children.length; i++) {
                        children[i] = readValue(reader);
                    }
                    break;
                case IValueIDs.NodeValue.KWPARAMS:
                    kwParams = readNamedValues(reader);
                    break;
                case IValueIDs.NodeValue.ANNOS:
                    annos = readNamedValues(reader);
                    break;
            }
        }

        INode node = vf.node(name, children);

        if (!annos.isEmpty()) {
            kwParams = simulateAnnotationsWithKeywordParameters(tf.nodeType(), kwParams, annos);
        }

        if (!kwParams.isEmpty()) {
            node = node.asWithKeywordParameters().setParameters(kwParams);
        }

        return returnAndStore(backReference, valueWindow, node);
    }

    private IValue readConstructor(final IWireInputStream reader) throws IOException {
        Type type = VOID_TYPE;
        IValue[] children = new IValue[0];
        Map.Immutable<String, IValue> annos = Map.Immutable.of();
        Map.Immutable<String, IValue> kwParams = Map.Immutable.of();


        boolean backReference = false;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()){
                case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                    backReference = true;
                    break;
                case IValueIDs.ConstructorValue.TYPE:
                    type = readType(reader);
                    break;
                case IValueIDs.ConstructorValue.PARAMS:
                    children = new IValue[reader.getRepeatedLength()];
                    switch (children.length) {
                        case 1 :
                            children[0] = readValue(reader);
                            break;
                        case 2 :
                            children[0] = readValue(reader);
                            children[1] = readValue(reader);
                            break;
                        case 3 :
                            children[0] = readValue(reader);
                            children[1] = readValue(reader);
                            children[2] = readValue(reader);
                            break;
                        default:
                            for (int i = 0; i < children.length; i++) {
                                children[i] = readValue(reader);
                            }
                            break;
                    }
                    assert paramsAreCorrectType(children, type);
                    break;
                case IValueIDs.ConstructorValue.KWPARAMS:
                    kwParams = readNamedValues(reader);
                    break;
                case IValueIDs.ConstructorValue.ANNOS:
                    annos = readNamedValues(reader);
                    break;
            }
        }

        if (type == VOID_TYPE) {
            throw new IOException("Constructor was missing type");
        }

        IConstructor constr = vf.constructor(type, children);

        if (!annos.isEmpty()) {
            kwParams = simulateAnnotationsWithKeywordParameters(constr.getType(), kwParams, annos);
        }

        if (!kwParams.isEmpty()) {
            constr = constr.asWithKeywordParameters().setParameters(kwParams);
        }

        return returnAndStore(backReference, valueWindow, constr);
    }

    /**
     * For bootstrapping purposes we still support annotations in the binary reader (not in the writer).
     * Here all annotations are converted to corresponding keyword parameters and the `Tree@\loc` annotation
     * is rewritten to `Tree.src`
     * @param receiver
     *
     * @deprecated
     * @param kwParams
     * @param annos
     * @return
     */
    @Deprecated
    private Immutable<String, IValue> simulateAnnotationsWithKeywordParameters(Type receiver, Immutable<String, IValue> kwParams, Immutable<String, IValue> annos) {
        for (Entry<String,IValue> entry : (Iterable<Entry<String, IValue>>) () -> annos.entryIterator()) {
            String key = entry.getKey();
            IValue value = entry.getValue();

            kwParams = kwParams.__put(isLegacyParseTreeLocAnnotation(key, receiver) ? "src" : key, value);
        }

        return kwParams;
    }

    private boolean isLegacyParseTreeLocAnnotation(String key, Type receiver) {
        return key.equals("loc") && receiver.isAbstractData() && receiver.getName().equals("Tree");
    }

    private boolean paramsAreCorrectType(IValue[] children, Type type) {
        assert children.length == type.getArity();
        for (int i = 0; i < children.length; i++) {
            if (!children[i].getType().isSubtypeOf(type.getFieldType(i))) {
                return false;
            }
        }
        return true;
    }

    private Map.Immutable<String, IValue> readNamedValues(IWireInputStream reader) throws IOException {
        Map.Transient<String, IValue> result = Map.Transient.of();
        String[] names = new String[0];
        reader.next();
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()){
                case IValueIDs.NamedValues.NAMES:
                    names = reader.getStrings();
                    break;
                case IValueIDs.NamedValues.VALUES:
                    assert names.length == reader.getRepeatedLength();
                    for (String name: names) {
                        result.__put(name, readValue(reader));
                    }
                    break;
            }
        }
        return result.freeze();
    }

    private IValue readString(final IWireInputStream reader) throws IOException {
        String str = "";
        boolean backReference = false;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()) {
                case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                    backReference = true;
                    break;
                case IValueIDs.StringValue.CONTENT:
                    str = reader.getString();
                    break;
                default:
                    reader.skipNestedField();
                    break;
            }
        }
        return returnAndStore(backReference, valueWindow, vf.string(str));
    }


    private IValue readReal(final IWireInputStream reader) throws IOException {
        byte[] bytes = new byte[0];
        int scale = 0;

        boolean backReference = false;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()){
                case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                    backReference = true;
                    break;
                case IValueIDs.RealValue.SCALE:
                    scale = reader.getInteger();
                    break;
                case IValueIDs.RealValue.CONTENT:
                    bytes = reader.getBytes();
                    break;
                default:
                    reader.skipNestedField();
                    break;
            }
        }

        return returnAndStore(backReference, valueWindow, vf.real(new BigDecimal(new BigInteger(bytes), scale).toString())); // TODO: Improve this?
    }


    private IValue readRational(final IWireInputStream reader) throws IOException {
        boolean backReference = false;
        IInteger denominator = vf.integer(0);
        IInteger numerator = vf.integer(0);
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()){
                case IValueIDs.Common.CAN_BE_BACK_REFERENCED:
                    backReference = true;
                    break;
                case IValueIDs.RationalValue.DENOMINATOR:
                    denominator = (IInteger) readValue(reader);
                    break;
                case IValueIDs.RationalValue.NUMERATOR:
                    numerator = (IInteger) readValue(reader);
                    break;
                default:
                    reader.skipNestedField();
                    break;
            }
        }

        return returnAndStore(backReference, valueWindow, vf.rational(numerator, denominator));
    }




    private IValue readSourceLocation(final IWireInputStream reader) throws IOException {
        String scheme = "";
        String authority = "";
        String path = "";
        String query = null;
        String fragment = null;
        int previousURI = -1;
        int offset = -1;
        int length = -1;
        int beginLine = -1;
        int endLine = -1;
        int beginColumn = -1;
        int endColumn = -1;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()){
                case IValueIDs.SourceLocationValue.PREVIOUS_URI:
                    previousURI = reader.getInteger();
                    break;
                case IValueIDs.SourceLocationValue.SCHEME:
                    scheme = reader.getString();
                    break;
                case IValueIDs.SourceLocationValue.AUTHORITY:
                    authority = reader.getString();
                    break;
                case IValueIDs.SourceLocationValue.PATH:
                    path = reader.getString();
                    break;
                case IValueIDs.SourceLocationValue.QUERY:
                    query = reader.getString();
                    break;
                case IValueIDs.SourceLocationValue.FRAGMENT:
                    fragment = reader.getString();
                    break;
                case IValueIDs.SourceLocationValue.OFFSET:
                    offset = reader.getInteger();
                    break;
                case IValueIDs.SourceLocationValue.LENGTH:
                    length = reader.getInteger();
                    break;
                case IValueIDs.SourceLocationValue.BEGINLINE:
                    beginLine = reader.getInteger();
                    break;
                case IValueIDs.SourceLocationValue.ENDLINE:
                    endLine = reader.getInteger();
                    break;
                case IValueIDs.SourceLocationValue.BEGINCOLUMN:
                    beginColumn = reader.getInteger();
                    break;
                case IValueIDs.SourceLocationValue.ENDCOLUMN:
                    endColumn = reader.getInteger();
                    break;
            }
        }
        ISourceLocation loc;
        if (previousURI != -1) {
            loc = uriWindow.lookBack(previousURI);
        }
        else {
            try {
                loc = vf.sourceLocation(scheme, authority, path, query, fragment);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
            uriWindow.read(loc);
        }

        if(beginLine >= 0){
            assert offset >= 0 && length >= 0 && endLine >= 0 && beginColumn >= 0 && endColumn >= 0;
            loc = vf.sourceLocation(loc, offset, length, beginLine, endLine, beginColumn, endColumn);
        } else if (offset >= 0){
            assert length >= 0;
            loc = vf.sourceLocation(loc, offset, length);
        }
        return loc;
    }



    private IValue readInteger(final IWireInputStream reader) throws IOException {
        @MonotonicNonNull Integer small = null;
        byte @MonotonicNonNull[] big = null;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()){
                case IValueIDs.IntegerValue.INTVALUE:
                    small = reader.getInteger();
                    break;
                case IValueIDs.IntegerValue.BIGVALUE:
                    big = reader.getBytes();
                    break;
            }
        }

        if(small != null){
            return vf.integer(small);
        } else if(big != null){
            return vf.integer(big);
        } else {
            throw new RuntimeException("Missing field in INT_VALUE");
        }
    }


    private IValue readDateTime(final IWireInputStream reader) throws IOException {
        int year = -1;
        int month = -1;
        int day = -1;

        int hour = -1;
        int minute = -1;
        int second = -1;
        int millisecond = -1;

        int timeZoneHourOffset = Integer.MAX_VALUE;
        int timeZoneMinuteOffset = Integer.MAX_VALUE;

        while (reader.next() != IWireInputStream.MESSAGE_END) {
            switch(reader.field()){
                case IValueIDs.DateTimeValue.YEAR:
                    year = reader.getInteger();
                    break;
                case IValueIDs.DateTimeValue.MONTH:
                    month = reader.getInteger();
                    break;
                case IValueIDs.DateTimeValue.DAY:
                    day = reader.getInteger();
                    break;
                case IValueIDs.DateTimeValue.HOUR:
                    hour = reader.getInteger();
                    break;
                case IValueIDs.DateTimeValue.MINUTE:
                    minute = reader.getInteger();
                    break;
                case IValueIDs.DateTimeValue.SECOND:
                    second = reader.getInteger();
                    break;
                case IValueIDs.DateTimeValue.MILLISECOND:
                    millisecond = reader.getInteger();
                    break;
                case IValueIDs.DateTimeValue.TZ_HOUR:
                    timeZoneHourOffset = reader.getInteger();
                    break;
                case IValueIDs.DateTimeValue.TZ_MINUTE:
                    timeZoneMinuteOffset = reader.getInteger();
                    break;
            }
        }


        if (hour != -1 && year != -1) {
            return vf.datetime(year, month, day, hour, minute, second, millisecond, timeZoneHourOffset, timeZoneMinuteOffset);
        }
        else if (hour != -1) {
            return vf.time(hour, minute, second, millisecond, timeZoneHourOffset, timeZoneMinuteOffset);
        }
        else {
            assert year != -1;
            return vf.date(year, month, day);
        }
    }


    private IValue readBoolean(final IWireInputStream reader) throws IOException {
        boolean value = false;
        while (reader.next() != IWireInputStream.MESSAGE_END) {
            if(reader.field() == IValueIDs.BoolValue.VALUE){
                value = true;
            }
            else {
                reader.skipNestedField();
            }
        }

        return vf.bool(value);
    }

}
