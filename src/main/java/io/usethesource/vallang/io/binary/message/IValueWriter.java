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
import java.util.HashSet;

import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.io.binary.stream.IValueOutputStream;
import io.usethesource.vallang.io.binary.util.StacklessStructuredVisitor;
import io.usethesource.vallang.io.binary.util.StructuredIValueVisitor;
import io.usethesource.vallang.io.binary.util.TrackLastWritten;
import io.usethesource.vallang.io.binary.util.WindowCacheFactory;
import io.usethesource.vallang.io.binary.util.WindowSizes;
import io.usethesource.vallang.io.binary.wire.IWireOutputStream;
import io.usethesource.vallang.type.ITypeVisitor;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeStore;

/**
 * An utility class for the {@link IValueOutputStream}. Only directly use methods in this class if you have nested IValues in an exisiting {@link IWireOutputStream}.
 *
 */
public class IValueWriter {
    /**
     * Write an IValue to an exisiting wire stream. <br />
     * <br />
     * In most cases you want to use the {@linkplain IValueOutputStream}.
     *  
     * @param writer the wire writer to use
     * @param vf the value factory used to rewrite external value types
     * @param size the window sizes to use
     * @param value the value to write   @throws IOException
     */
    public static void write(IWireOutputStream writer, IValueFactory vf, WindowSizes size, IValue value) throws IOException {
        final WindowCacheFactory windowFactory = WindowCacheFactory.getInstance();
        TrackLastWritten<Type> typeCache = windowFactory.getTrackLastWrittenReferenceEquality(size.typeWindow);
        TrackLastWritten<IValue> valueCache = windowFactory.getTrackLastWrittenReferenceEquality(size.valueWindow);
        TrackLastWritten<ISourceLocation> uriCache = windowFactory.getTrackLastWrittenReferenceEquality(size.uriWindow);
        try {
            writeHeader(writer, size.valueWindow, size.typeWindow, size.uriWindow);
            writer.writeNestedField(IValueIDs.Header.VALUE);
            write(writer, vf, value, typeCache, valueCache, uriCache);
            writer.endMessage();
        } finally {
            windowFactory.returnTrackLastWrittenReferenceEquality(typeCache);
            windowFactory.returnTrackLastWrittenReferenceEquality(valueCache);
            windowFactory.returnTrackLastWrittenReferenceEquality(uriCache);
        }
    }

    /**
     * Write an Type to an existing wire stream.
     *  
     * @param writer the wire writer to use
     * @param vf the value factory used to rewrite external value types
     * @param size the window sizes to use
     * @param type the type to write
     * @throws IOException
     */
    public static void write(IWireOutputStream writer, IValueFactory vf, WindowSizes size, Type type) throws IOException {
        final WindowCacheFactory windowFactory = WindowCacheFactory.getInstance();
        TrackLastWritten<Type> typeCache = windowFactory.getTrackLastWrittenReferenceEquality(size.typeWindow);
        TrackLastWritten<IValue> valueCache = windowFactory.getTrackLastWrittenReferenceEquality(size.valueWindow);
        TrackLastWritten<ISourceLocation> uriCache = windowFactory.getTrackLastWrittenReferenceEquality(size.uriWindow);
        try {
            writeHeader(writer, size.valueWindow, size.typeWindow, size.uriWindow);
            writer.writeNestedField(IValueIDs.Header.TYPE);
            write(writer, vf, type, typeCache, valueCache, uriCache);
            writer.endMessage();
        } finally {
            windowFactory.returnTrackLastWrittenReferenceEquality(typeCache);
            windowFactory.returnTrackLastWrittenReferenceEquality(valueCache);
            windowFactory.returnTrackLastWrittenReferenceEquality(uriCache);
        }
    }
    

    private static void writeHeader(IWireOutputStream writer, int valueWindowSize, int typeWindowSize, int uriWindowSize) throws IOException {
        writer.startMessage(IValueIDs.Header.ID);
        writer.writeField(IValueIDs.Header.VALUE_WINDOW, valueWindowSize);
        writer.writeField(IValueIDs.Header.TYPE_WINDOW, typeWindowSize);
        writer.writeField(IValueIDs.Header.SOURCE_LOCATION_WINDOW, uriWindowSize);
    }

    private static void write(final IWireOutputStream writer, IValueFactory vf, final Type type, final TrackLastWritten<Type> typeCache, final TrackLastWritten<IValue> valueCache, final TrackLastWritten<ISourceLocation> uriCache) throws IOException {
        type.accept(new ITypeVisitor<Void, IOException>() {

            private boolean writeFromCache(Type type) throws IOException {
                int lastSeen = typeCache.howLongAgo(type);
                if (lastSeen != -1) { 
                    writeSingleValueMessage(writer, IValueIDs.PreviousType.ID, IValueIDs.PreviousType.HOW_LONG_AGO, lastSeen);
                    return true;
                }
                return false;
            }

            @Override
            public Void visitAbstractData(Type type) throws IOException {
                if (writeFromCache(type)) {
                    return null;
                }
                writer.startMessage(IValueIDs.ADTType.ID);
                writeCanBeBackReferenced(writer);
                writer.writeField(IValueIDs.ADTType.NAME, type.getName());

                writer.writeNestedField(IValueIDs.ADTType.TYPE_PARAMS);
                type.getTypeParameters().accept(this);

                writer.endMessage();
                typeCache.write(type);
                return null;
            }

            @Override
            public Void visitAlias(Type type) throws IOException {
                if (writeFromCache(type)) {
                    return null;
                }
                writer.startMessage(IValueIDs.AliasType.ID);
                writeCanBeBackReferenced(writer);
                writer.writeField(IValueIDs.AliasType.NAME, type.getName());

                writer.writeNestedField(IValueIDs.AliasType.ALIASED);
                type.getAliased().accept(this);

                writer.writeNestedField(IValueIDs.AliasType.TYPE_PARAMS);
                type.getTypeParameters().accept(this);

                writer.endMessage();
                typeCache.write(type);
                return null;
            }

            @Override
            public Void visitConstructor(Type type) throws IOException {
                if (writeFromCache(type)) {
                    return null;
                }
                writer.startMessage(IValueIDs.ConstructorType.ID);
                writeCanBeBackReferenced(writer);
                writer.writeField(IValueIDs.ConstructorType.NAME, type.getName());

                writer.writeNestedField(IValueIDs.ConstructorType.ADT);
                type.getAbstractDataType().accept(this);

                writer.writeNestedField(IValueIDs.ConstructorType.FIELD_TYPES);
                type.getFieldTypes().accept(this);

                writer.endMessage();
                typeCache.write(type);
                return null;
            }

            @Override
            public Void visitExternal(Type type) throws IOException {
                if (writeFromCache(type)) {
                    return null;
                }
                writer.startMessage(IValueIDs.ExternalType.ID);
                writeCanBeBackReferenced(writer);

                writer.writeNestedField(IValueIDs.ExternalType.SYMBOL);
                IConstructor symbol = type.asSymbol(vf, new TypeStore(), vf.setWriter(), new HashSet<>());
                write(writer, vf, symbol, typeCache, valueCache, uriCache);

                writer.endMessage();
                typeCache.write(type);
                return null;
            }

            @Override
            public Void visitList(Type type) throws IOException {
                if (writeFromCache(type)) {
                    return null;
                }
                writer.startMessage(IValueIDs.ListType.ID);
                writeCanBeBackReferenced(writer);

                writer.writeNestedField(IValueIDs.ListType.ELEMENT_TYPE);
                type.getElementType().accept(this);

                writer.endMessage();
                typeCache.write(type);
                return null;
            }

            @Override
            public Void visitMap(Type type) throws IOException {
                if (writeFromCache(type)) {
                    return null;
                }

                writer.startMessage(IValueIDs.MapType.ID);
                writeCanBeBackReferenced(writer);

                writer.writeNestedField(IValueIDs.MapType.KEY_TYPE);
                type.getKeyType().accept(this);
                writer.writeNestedField(IValueIDs.MapType.VALUE_TYPE);
                type.getValueType().accept(this);

                writer.endMessage();
                typeCache.write(type);
                return null;
            }

            @Override
            public Void visitParameter(Type type) throws IOException {
                if (writeFromCache(type)) {
                    return null;
                }
                writer.startMessage(IValueIDs.ParameterType.ID);
                writeCanBeBackReferenced(writer);
                writer.writeField(IValueIDs.ParameterType.NAME, type.getName());

                writer.writeNestedField(IValueIDs.ParameterType.BOUND);
                type.getBound().accept(this);

                writer.endMessage();
                typeCache.write(type);
                return null;
            }

            @Override
            public Void visitSet(Type type) throws IOException {
                if (writeFromCache(type)) {
                    return null;
                }
                writer.startMessage(IValueIDs.SetType.ID);
                writeCanBeBackReferenced(writer);

                writer.writeNestedField(IValueIDs.SetType.ELEMENT_TYPE);
                type.getElementType().accept(this);

                writer.endMessage();
                typeCache.write(type);
                return null;
            }

            @Override
            public Void visitTuple(Type type) throws IOException {
                if (writeFromCache(type)) {
                    return null;
                }
                writer.startMessage(IValueIDs.TupleType.ID);
                writeCanBeBackReferenced(writer);


                writer.writeRepeatedNestedField(IValueIDs.TupleType.TYPES, type.getArity());
                for(int i = 0; i < type.getArity(); i++){
                    type.getFieldType(i).accept(this);
                }


                if( type.hasFieldNames()){
                    writer.writeField(IValueIDs.TupleType.NAMES, type.getFieldNames());
                }

                writer.endMessage();
                typeCache.write(type);
                return null;
            }

            @Override
            public Void visitBool(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.BoolType.ID);
                return null;
            }

            @Override
            public Void visitDateTime(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.DateTimeType.ID);
                return null;
            }

            @Override
            public Void visitInteger(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.IntegerType.ID);
                return null;
            }

            @Override
            public Void visitNode(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.NodeType.ID);
                return null;
            }

            @Override
            public Void visitNumber(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.NumberType.ID);
                return null;
            }

            @Override
            public Void visitRational(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.RationalType.ID);
                return null;
            }

            @Override
            public Void visitReal(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.RealType.ID);
                return null;
            }

            @Override
            public Void visitSourceLocation(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.SourceLocationType.ID);
                return null;
            }

            @Override
            public Void visitString(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.StringType.ID);
                return null;
            }

            @Override
            public Void visitValue(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.ValueType.ID);
                return null;
            }

            @Override
            public Void visitVoid(Type t) throws IOException {
                writer.writeEmptyMessage(IValueIDs.VoidType.ID);
                return null;
            }
        });
    }
    
    private static void writeSingleValueMessage(final IWireOutputStream writer, int messageID, int fieldId, int fieldValue) throws IOException {
        writer.startMessage(messageID);
        writer.writeField(fieldId, fieldValue);
        writer.endMessage();
    }
    
    private static void writeSingleValueMessage(final IWireOutputStream writer, int messageID, int fieldId, String fieldValue) throws IOException {
        writer.startMessage(messageID);
        writer.writeField(fieldId, fieldValue);
        writer.endMessage();
    }
    private static void writeCanBeBackReferenced(final IWireOutputStream writer) throws IOException {
        writer.writeField(IValueIDs.Common.CAN_BE_BACK_REFERENCED, 1);
    }

    private static void write(final IWireOutputStream writer, IValueFactory vf, final IValue value, final TrackLastWritten<Type> typeCache, final TrackLastWritten<IValue> valueCache, final TrackLastWritten<ISourceLocation> uriCache) throws IOException {
        final IInteger MININT = vf.integer(Integer.MIN_VALUE);
        final IInteger MAXINT = vf.integer(Integer.MAX_VALUE);

        StacklessStructuredVisitor.accept(value, new StructuredIValueVisitor<IOException>() {

            private boolean writeFromCache(IValue val) throws IOException {
                int lastSeen = valueCache.howLongAgo(val);
                if (lastSeen != -1) {
                    writeSingleValueMessage(writer, IValueIDs.PreviousValue.ID, IValueIDs.PreviousValue.HOW_FAR_BACK, lastSeen);
                    return true;
                }
                return false;
            }

            @Override
            public boolean enterConstructor(IConstructor cons, int children) throws IOException {
                if (writeFromCache(cons)) {
                    return false;
                }
                writer.startMessage(IValueIDs.ConstructorValue.ID);
                writeCanBeBackReferenced(writer);

                writer.writeNestedField(IValueIDs.ConstructorValue.TYPE);
                write(writer, vf, cons.getUninstantiatedConstructorType(), typeCache, valueCache, uriCache);

                if (children > 0) {
                    writer.writeRepeatedNestedField(IValueIDs.ConstructorValue.PARAMS, children);
                }

                return true;
            }

            @Override
            public void enterConstructorKeywordParameters() throws IOException {
                writer.writeNestedField(IValueIDs.ConstructorValue.KWPARAMS);
            }

            @Override
            public void leaveConstructor(IValue cons) throws IOException {
                writer.endMessage();
                valueCache.write(cons);
            }

            @Override
            public boolean enterNode(INode node, int children) throws IOException {
                if (writeFromCache(node)) {
                    return false;
                }
                writer.startMessage(IValueIDs.NodeValue.ID);
                writeCanBeBackReferenced(writer);
                writer.writeField(IValueIDs.NodeValue.NAME, node.getName());
                if (children > 0) {
                    writer.writeRepeatedNestedField(IValueIDs.NodeValue.PARAMS, children);
                }
                return true;
            }

            @Override
            public void enterNodeKeywordParameters() throws IOException {
                writer.writeNestedField(IValueIDs.NodeValue.KWPARAMS);
            }

            @Override
            public void leaveNode(IValue cons) throws IOException {
                writer.endMessage();
                valueCache.write(cons);
            }

            @Override
            public void enterNamedValues(String[] names, int numberOfNestedValues) throws IOException {
                writer.startMessage(IValueIDs.NamedValues.ID);
                writer.writeField(IValueIDs.NamedValues.NAMES, names);
                writer.writeRepeatedNestedField(IValueIDs.NamedValues.VALUES, numberOfNestedValues);
            }

            @Override
            public void leaveNamedValue() throws IOException {
                writer.endMessage();
            }


            @Override
            public boolean enterList(IList lst, int children) throws IOException {
                if (writeFromCache(lst)) {
                    return false;
                }
                writer.startMessage(IValueIDs.ListValue.ID);
                writeCanBeBackReferenced(writer);
                writer.writeRepeatedNestedField(IValueIDs.ListValue.ELEMENTS, children);
                return true;
            }

            @Override
            public void leaveList(IValue lst) throws IOException {
                writer.endMessage();
                valueCache.write(lst);
            }

            @Override
            public boolean enterSet(ISet lst, int elements) throws IOException {
                if (writeFromCache(lst)) {
                    return false;
                }
                writer.startMessage(IValueIDs.SetValue.ID);
                writeCanBeBackReferenced(writer);
                writer.writeRepeatedNestedField(IValueIDs.SetValue.ELEMENTS, elements);
                return true;
            }

            @Override
            public void leaveSet(IValue lst) throws IOException {
                writer.endMessage();
                valueCache.write(lst);
            }

            @Override
            public boolean enterMap(IMap map, int elements) throws IOException {
                if (writeFromCache(map)) {
                    return false;
                }
                writer.startMessage(IValueIDs.MapValue.ID);
                writeCanBeBackReferenced(writer);
                writer.writeRepeatedNestedField(IValueIDs.MapValue.KV_PAIRS, elements * 2);
                return true;
            }

            @Override
            public void leaveMap(IValue map) throws IOException {
                writer.endMessage();
                valueCache.write(map);
            }

            @Override
            public boolean enterTuple(ITuple tuple, int arity) throws IOException {
                if (writeFromCache(tuple)) {
                    return false;
                }
                writer.startMessage(IValueIDs.TupleValue.ID);
                writeCanBeBackReferenced(writer);
                writer.writeRepeatedNestedField(IValueIDs.TupleValue.CHILDREN, arity);
                return true;
            }

            @Override
            public void leaveTuple(IValue tuple) throws IOException {
                writer.endMessage();
                valueCache.write(tuple);
            }

            @Override
            public void visitBoolean(IBool boolValue) throws IOException {
                if (boolValue.getValue()) {
                    writeSingleValueMessage(writer, IValueIDs.BoolValue.ID, IValueIDs.BoolValue.VALUE, 1);
                }
                else {
                    writer.writeEmptyMessage(IValueIDs.BoolValue.ID);
                }
            }

            @Override
            public void visitDateTime(IDateTime dateTime) throws IOException {
                writer.startMessage(IValueIDs.DateTimeValue.ID);

                if (!dateTime.isTime()) {
                    writer.writeField(IValueIDs.DateTimeValue.YEAR, dateTime.getYear());
                    writer.writeField(IValueIDs.DateTimeValue.MONTH, dateTime.getMonthOfYear());
                    writer.writeField(IValueIDs.DateTimeValue.DAY, dateTime.getDayOfMonth());
                }

                if (!dateTime.isDate()) {
                    writer.writeField(IValueIDs.DateTimeValue.HOUR, dateTime.getHourOfDay());
                    writer.writeField(IValueIDs.DateTimeValue.MINUTE, dateTime.getMinuteOfHour());
                    writer.writeField(IValueIDs.DateTimeValue.SECOND, dateTime.getSecondOfMinute());
                    writer.writeField(IValueIDs.DateTimeValue.MILLISECOND, dateTime.getMillisecondsOfSecond());

                    writer.writeField(IValueIDs.DateTimeValue.TZ_HOUR, dateTime.getTimezoneOffsetHours());
                    writer.writeField(IValueIDs.DateTimeValue.TZ_MINUTE, dateTime.getTimezoneOffsetMinutes());
                }
                writer.endMessage();
            }
            @Override
            public void visitInteger(IInteger ii) throws IOException {
                writer.startMessage(IValueIDs.IntegerValue.ID);
                if(ii. greaterEqual(MININT).getValue() && ii.lessEqual(MAXINT).getValue()){
                    writer.writeField(IValueIDs.IntegerValue.INTVALUE, ii.intValue());
                } 
                else {
                    writer.writeField(IValueIDs.IntegerValue.BIGVALUE, ii.getTwosComplementRepresentation());
                }
                writer.endMessage();
            }


            @Override
            public void visitReal(IReal o) throws IOException {
                writer.startMessage(IValueIDs.RealValue.ID);
                writer.writeField(IValueIDs.RealValue.CONTENT, o.unscaled().getTwosComplementRepresentation());
                writer.writeField(IValueIDs.RealValue.SCALE, o.scale());
                writer.endMessage();
            }

            @Override
            public void visitSourceLocation(ISourceLocation loc) throws IOException {
                writer.startMessage(IValueIDs.SourceLocationValue.ID);
                ISourceLocation uriPart = loc.top();
                int alreadyWritten = uriCache.howLongAgo(uriPart);
                if (alreadyWritten == -1) {
                    writer.writeField(IValueIDs.SourceLocationValue.SCHEME, uriPart.getScheme());
                    if (uriPart.hasAuthority()) {
                        writer.writeField(IValueIDs.SourceLocationValue.AUTHORITY, uriPart.getAuthority());
                    }
                    if (uriPart.hasPath()) {
                        writer.writeField(IValueIDs.SourceLocationValue.PATH, uriPart.getPath());
                    }
                    if (uriPart.hasQuery()) {
                        writer.writeField(IValueIDs.SourceLocationValue.QUERY,  uriPart.getQuery());
                    }
                    if (uriPart.hasFragment()) {
                        writer.writeField(IValueIDs.SourceLocationValue.FRAGMENT,  uriPart.getFragment());
                    }
                    uriCache.write(uriPart);
                }
                else {
                    writer.writeField(IValueIDs.SourceLocationValue.PREVIOUS_URI, alreadyWritten);
                }

                if(loc.hasOffsetLength()){
                    writer.writeField(IValueIDs.SourceLocationValue.OFFSET, loc.getOffset());
                    writer.writeField(IValueIDs.SourceLocationValue.LENGTH, loc.getLength());
                } 
                if(loc.hasLineColumn()){
                    writer.writeField(IValueIDs.SourceLocationValue.BEGINLINE, loc.getBeginLine());
                    writer.writeField(IValueIDs.SourceLocationValue.ENDLINE, loc.getEndLine());
                    writer.writeField(IValueIDs.SourceLocationValue.BEGINCOLUMN, loc.getBeginColumn());
                    writer.writeField(IValueIDs.SourceLocationValue.ENDCOLUMN, loc.getEndColumn());
                }
                writer.endMessage();
            }

            @Override
            public void visitString(IString o) throws IOException {
                // TODO: if `o` is a really big binary tree string this o.getValue() duplicates the memory consumption, and it can be slow because of this. 
                // We can then optimize using the writer interface or the character iterators of IString 
                writeSingleValueMessage(writer, IValueIDs.StringValue.ID, IValueIDs.StringValue.CONTENT, o.getValue());
            }
            
            @Override
            public void visitRational(IRational val) throws IOException {
                writer.startMessage(IValueIDs.RationalValue.ID); 
                writer.writeNestedField(IValueIDs.RationalValue.NUMERATOR);
                visitInteger(val.numerator());
                writer.writeNestedField(IValueIDs.RationalValue.DENOMINATOR);
                visitInteger(val.denominator());
                writer.endMessage();
            }
        });
    }

}
