/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.io.binary;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IDateTime;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.util.IndexedSet;

// TODO Change this thing so it doesn't use recursion.
/**
 * @author Arnold Lankamp
 */
public class BinaryWriter{
	/*package*/ static final String CharEncoding = "UTF8";
	private final static int BOOL_HEADER = 0x01;
//	private final static int INTEGER_HEADER = 0x02;
	private final static int BIG_INTEGER_HEADER = 0x03; // Special case of INTEGER_HEADER (flags for alternate encoding).
	private final static int DOUBLE_HEADER = 0x04;
	private final static int STRING_HEADER = 0x05;
	private final static int SOURCE_LOCATION_HEADER = 0x06;
	private final static int DATE_TIME_HEADER = 0x10;
	private final static int TUPLE_HEADER = 0x07;
	private final static int NODE_HEADER = 0x08;
	private final static int ANNOTATED_NODE_HEADER = 0x09;
	private final static int CONSTRUCTOR_HEADER = 0x0a;
	private final static int ANNOTATED_CONSTRUCTOR_HEADER = 0x0b;
	private final static int LIST_HEADER = 0x0c;
	private final static int SET_HEADER = 0x0d;
	private final static int RELATION_HEADER = 0x0e;
	private final static int MAP_HEADER = 0x0f;
	private final static int RATIONAL_HEADER = 0x11;

	private final static int VALUE_TYPE_HEADER = 0x01;
	private final static int VOID_TYPE_HEADER = 0x02;
	private final static int BOOL_TYPE_HEADER = 0x03;
	private final static int INTEGER_TYPE_HEADER = 0x04;
	private final static int DOUBLE_TYPE_HEADER = 0x05;
	private final static int STRING_TYPE_HEADER = 0x06;
	private final static int SOURCE_LOCATION_TYPE_HEADER = 0x07;
	private final static int DATE_TIME_TYPE_HEADER = 0x14;
	private final static int NODE_TYPE_HEADER = 0x08;
	private final static int TUPLE_TYPE_HEADER = 0x09;
	private final static int LIST_TYPE_HEADER = 0x0a;
	private final static int SET_TYPE_HEADER = 0x0b;
	private final static int RELATION_TYPE_HEADER = 0x0c;
	private final static int MAP_TYPE_HEADER = 0x0d;
	private final static int PARAMETER_TYPE_HEADER = 0x0e;
	private final static int ADT_TYPE_HEADER = 0x0f;
	private final static int CONSTRUCTOR_TYPE_HEADER = 0x10;
	private final static int ALIAS_TYPE_HEADER = 0x11;
	private final static int ANNOTATED_NODE_TYPE_HEADER = 0x12;
	private final static int ANNOTATED_CONSTRUCTOR_TYPE_HEADER = 0x13;
	private final static int RATIONAL_TYPE_HEADER = 0x15;
	
	private final static int SHARED_FLAG = 0x80;
	private final static int TYPE_SHARED_FLAG = 0x40;
	private final static int URL_SHARED_FLAG = 0x20;
	private final static int NAME_SHARED_FLAG = 0x20;
	
	private final static int HAS_FIELD_NAMES = 0x20;
	
	private final static int DATE_TIME_INDICATOR = 0x01;
	private final static int DATE_INDICATOR = 0x02;
	private final static int TIME_INDICATOR = 0x03;
	
	private final IndexedSet<IValue> sharedValues;
	private final IndexedSet<Type> sharedTypes;
	private final IndexedSet<String> sharedPaths;
	private final IndexedSet<String> sharedNames;
	
	private final IValue value;
	private final OutputStream out;
	private final TypeStore typeStore;
	
	public BinaryWriter(IValue value, OutputStream outputStream, TypeStore typeStore){
		super();
		
		this.value = value;
		this.out = outputStream;
		this.typeStore = typeStore;
		
		sharedValues = new IndexedSet<IValue>();
		sharedTypes = new IndexedSet<Type>();
		sharedPaths = new IndexedSet<String>();
		sharedNames = new IndexedSet<String>();
	}
	
	public void serialize() throws IOException{
		doSerialize(value);
	}
	
	private void doSerialize(IValue value) throws IOException{
		// This special cases the hashing logic: if we have a constructor with
		// at least one location annotation, don't try to hash it
		boolean tryHashing = true;
		
		if (value.getType().isAbstractDataType()) {
			IConstructor consValue = (IConstructor)value;
			if (consValue.hasAnnotations()) {
				Map<String,IValue> amap = consValue.getAnnotations();
				for (String akey : amap.keySet()) {
					Type aType = amap.get(akey).getType();
					if (!aType.isVoidType() && aType.isSourceLocationType()) {
						tryHashing = false;
						break;
					}
				}
			}
		}
		
		if (tryHashing) {
			int valueId = sharedValues.get(value);
			if(valueId != -1){
				out.write(SHARED_FLAG);
				printInteger(valueId);
				return;
			}
		}
		
		// This sucks and is order dependent :-/.
		if(value instanceof IBool){
			writeBool((IBool) value);
		}else if(value instanceof IInteger){
			writeInteger((IInteger) value);
		}else if(value instanceof IRational){
			writeRational((IRational) value);
		}else if(value instanceof IReal){
			writeDouble((IReal) value);
		}else if(value instanceof IString){
			writeString((IString) value);
		}else if(value instanceof ISourceLocation){
			writeSourceLocation((ISourceLocation) value);
		}else if(value instanceof IDateTime){
			writeDateTime((IDateTime) value);
		}else if(value instanceof ITuple){
			writeTuple((ITuple) value);
		}else if(value instanceof IConstructor){
			IConstructor constructor = (IConstructor) value;
			if(!constructor.hasAnnotations()) writeConstructor(constructor);
			else writeAnnotatedConstructor(constructor);
		}else if(value instanceof INode){
			INode node = (INode) value;
			if(!node.hasAnnotations()) writeNode(node);
			else writeAnnotatedNode(node);
		}else if(value instanceof IList){
			writeList((IList) value);
		}else if(value instanceof IRelation){
			writeRelation((IRelation) value);
		}else if(value instanceof ISet){
			writeSet((ISet) value);
		}else if(value instanceof IMap){
			writeMap((IMap) value);
		}
		
		if (tryHashing) {
			sharedValues.store(value);
		}
	}
	
	private void doWriteType(Type type) throws IOException{
		// This sucks and is order dependent :-/.
		if(type.isVoidType()){
			writeVoidType();
		}else if(type.isAliasType()){
			writeAliasType(type);
		}else if(type.isParameterType()){
			writeParameterType(type);
		}else if(type.isValueType()){
			writeValueType();
		}else if(type.isBoolType()){
			writeBoolType();
		}else if(type.isIntegerType()){
			writeIntegerType();
		}else if(type.isRealType()){
			writeDoubleType();
		}else if(type.isRationalType()){
			writeRationalType();
		}else if(type.isStringType()){
			writeStringType();
		}else if(type.isSourceLocationType()){
			writeSourceLocationType();
		}else if(type.isDateTimeType()){
			writeDateTimeType(type);
		}else if(type.isListType()){
			writeListType(type);
		}else if(type.isSetType()){
			writeSetType(type);
		}else if(type.isRelationType()){
			writeRelationType(type);
		}else if(type.isMapType()){
			writeMapType(type);
		}else if(type.isAbstractDataType()){
			writeADTType(type);
		}else if(type.isConstructorType()){
			writeConstructorType(type);
		}else if(type.isNodeType()){
			writeNodeType(type);
		}else if(type.isTupleType()){
			writeTupleType(type);
		}
	}
	
	private void writeType(Type type) throws IOException{
		int typeId = sharedTypes.get(type);
		if(typeId != -1){
			out.write(SHARED_FLAG);
			printInteger(typeId);
			return;
		}
		
		doWriteType(type);
		
		sharedTypes.store(type);
	}
	
	private void writeBool(IBool bool) throws IOException{
		out.write(BOOL_HEADER);
		
		if(bool.getValue()){
			out.write(1);
		}else{
			out.write(0);
		}
	}
	
	private void writeInteger(IInteger integer) throws IOException{
		byte[] valueData = integer.getTwosComplementRepresentation();
		int length = valueData.length;
		out.write(BIG_INTEGER_HEADER);
		printInteger(length);
		out.write(valueData, 0, length);
	}

	/**
	 *  Format:
	 *    header
	 *    length of numerator
	 *    numerator byte[]
	 *    length of denominator
	 *    denominator byte[]
	 */
	private void writeRational(IRational rational) throws IOException{
		out.write(RATIONAL_HEADER);
		
		byte[] valueData = rational.numerator().getTwosComplementRepresentation();
		int length = valueData.length;
		printInteger(length);
		out.write(valueData, 0, length);

		valueData = rational.denominator().getTwosComplementRepresentation();
		length = valueData.length;
		printInteger(length);
		out.write(valueData, 0, length);
		
	}
	private void writeDouble(IReal real) throws IOException{
		out.write(DOUBLE_HEADER);
		
		byte[] valueData = real.unscaled().getTwosComplementRepresentation();
		int length = valueData.length;
		printInteger(length);
		out.write(valueData, 0, length);
		
		printInteger(real.scale());
	}
	
	private void writeString(IString string) throws IOException{
		out.write(STRING_HEADER);
		
		String theString = string.getValue();
		
		byte[] stringData = theString.getBytes(CharEncoding);
		printInteger(stringData.length);
		out.write(stringData);
	}
	
	private void writeSourceLocation(ISourceLocation sourceLocation) throws IOException{
		URI uri = sourceLocation.getURI();
		String path = uri.toString();
		int id = sharedPaths.store(path);
		
		int header = SOURCE_LOCATION_HEADER;
		
		if(id == -1){
			out.write(header);
			
			byte[] pathData = path.getBytes(CharEncoding);
			printInteger(pathData.length);
			out.write(pathData);
		} else{
			out.write(header | URL_SHARED_FLAG);
			
			printInteger(id);
		}
		
		int beginLine, beginColumn, endLine, endColumn;
		
		if (!sourceLocation.hasLineColumn()) {
			beginLine = -1;
			endLine = -1;
			beginColumn = -1;
			endColumn = -1;
		}
		else {
			beginLine = sourceLocation.getBeginLine();
			endLine = sourceLocation.getEndLine();
			beginColumn = sourceLocation.getBeginColumn();
			endColumn = sourceLocation.getEndColumn();
		}
		
		int offset, length;
		if (!sourceLocation.hasOffsetLength()) {
			offset = -1;
			length = -1;
		}
		else {
			offset = sourceLocation.getOffset();
			length = sourceLocation.getLength();
		}
		
		printInteger(offset);
		printInteger(length);
		printInteger(beginLine);
		printInteger(endLine);
		printInteger(beginColumn);
		printInteger(endColumn);
	}
	
	private void writeDateTime(IDateTime dateTime) throws IOException{
		out.write(DATE_TIME_HEADER);
		
		if(dateTime.isDateTime()){
			out.write(DATE_TIME_INDICATOR);
			
			printInteger(dateTime.getYear());
			printInteger(dateTime.getMonthOfYear());
			printInteger(dateTime.getDayOfMonth());
			
			printInteger(dateTime.getHourOfDay());
			printInteger(dateTime.getMinuteOfHour());
			printInteger(dateTime.getSecondOfMinute());
			printInteger(dateTime.getMillisecondsOfSecond());
			
			printInteger(dateTime.getTimezoneOffsetHours());
			printInteger(dateTime.getTimezoneOffsetMinutes());
		}else if(dateTime.isDate()){
			out.write(DATE_INDICATOR);
			
			printInteger(dateTime.getYear());
			printInteger(dateTime.getMonthOfYear());
			printInteger(dateTime.getDayOfMonth());
		}else{
			out.write(TIME_INDICATOR);
			
			printInteger(dateTime.getHourOfDay());
			printInteger(dateTime.getMinuteOfHour());
			printInteger(dateTime.getSecondOfMinute());
			printInteger(dateTime.getMillisecondsOfSecond());
			
			printInteger(dateTime.getTimezoneOffsetHours());
			printInteger(dateTime.getTimezoneOffsetMinutes());
		}
	}
	
	private void writeTuple(ITuple tuple) throws IOException{
		out.write(TUPLE_HEADER);
		
		int arity = tuple.arity();
		printInteger(arity);
		
		for(int i = 0; i < arity; i++){
			doSerialize(tuple.get(i));
		}
	}
	
	private void writeNode(INode node) throws IOException{
		String nodeName = node.getName();
		int nodeNameId = sharedNames.store(nodeName);
		
		if(nodeNameId == -1){
			out.write(NODE_HEADER);
			
			byte[] nodeData = nodeName.getBytes(CharEncoding);
			printInteger(nodeData.length);
			out.write(nodeData);
		}else{
			out.write(NODE_HEADER | NAME_SHARED_FLAG);
			
			printInteger(nodeNameId);
		}
		
		int arity = node.arity();
		printInteger(arity);
		
		for(int i = 0; i < arity; i++){
			doSerialize(node.get(i));
		}
	}
	
	private void writeAnnotatedNode(INode node) throws IOException{
		String nodeName = node.getName();
		int nodeNameId = sharedNames.store(nodeName);
		
		if(nodeNameId == -1){
			out.write(ANNOTATED_NODE_HEADER);
			
			byte[] nodeData = nodeName.getBytes(CharEncoding);
			printInteger(nodeData.length);
			out.write(nodeData);
		}else{
			out.write(ANNOTATED_NODE_HEADER | NAME_SHARED_FLAG);
			
			printInteger(nodeNameId);
		}
		
		int arity = node.arity();
		printInteger(arity);
		
		for(int i = 0; i < arity; i++){
			doSerialize(node.get(i));
		}
		
		Map<String, IValue> annotations = node.getAnnotations();
		
		printInteger(annotations.size());
		
		Iterator<Map.Entry<String, IValue>> annotationsIterator = annotations.entrySet().iterator();
		while(annotationsIterator.hasNext()){
			Map.Entry<String, IValue> annotation = annotationsIterator.next();
			String label = annotation.getKey();
			byte[] labelData = label.getBytes(CharEncoding);
			printInteger(labelData.length);
			out.write(labelData);
			
			IValue value = annotation.getValue();
			doSerialize(value);
		}
	}
	
	private void writeConstructor(IConstructor constructor) throws IOException{
		Type constructorType = constructor.getConstructorType();
		int constructorTypeId = sharedTypes.get(constructorType);
		
		if(constructorTypeId == -1){
			out.write(CONSTRUCTOR_HEADER);
			
			doWriteType(constructorType);
			
			sharedTypes.store(constructorType);
		}else{
			out.write(CONSTRUCTOR_HEADER | TYPE_SHARED_FLAG);
			
			printInteger(constructorTypeId);
		}
		
		int arity = constructor.arity();
		printInteger(arity);
		
		for(int i = 0; i < arity; i++){
			doSerialize(constructor.get(i));
		}
	}
	
	private void writeAnnotatedConstructor(IConstructor constructor) throws IOException{
		Type constructorType = constructor.getConstructorType();
		int constructorTypeId = sharedTypes.get(constructorType);
		
		if(constructorTypeId == -1){
			out.write(ANNOTATED_CONSTRUCTOR_HEADER);
			
			doWriteType(constructorType);
			
			sharedTypes.store(constructorType);
		}else{
			out.write(ANNOTATED_CONSTRUCTOR_HEADER | TYPE_SHARED_FLAG);
			
			printInteger(constructorTypeId);
		}
		
		int arity = constructor.arity();
		printInteger(arity);
		
		for(int i = 0; i < arity; i++){
			doSerialize(constructor.get(i));
		}
		
		Map<String, IValue> annotations = constructor.getAnnotations();
		
		printInteger(annotations.size());
		
		Iterator<Map.Entry<String, IValue>> annotationsIterator = annotations.entrySet().iterator();
		while(annotationsIterator.hasNext()){
			Map.Entry<String, IValue> annotation = annotationsIterator.next();
			String label = annotation.getKey();
			byte[] labelData = label.getBytes(CharEncoding);
			printInteger(labelData.length);
			out.write(labelData);
			
			IValue value = annotation.getValue();
			doSerialize(value);
		}
	}
	
	private void writeList(IList list) throws IOException{
		Type elementType = list.getElementType();
		int elementTypeId = sharedTypes.get(elementType);
		
		if(elementTypeId == -1){
			out.write(LIST_HEADER);
			
			doWriteType(elementType);
			
			sharedTypes.store(elementType);
		}else{
			out.write(LIST_HEADER | TYPE_SHARED_FLAG);
			
			printInteger(elementTypeId);
		}
		
		int length = list.length();
		printInteger(length);
		for(int i = 0; i < length; i++){
			doSerialize(list.get(i));
		}
	}
	
	private void writeSet(ISet set) throws IOException{
		Type elementType = set.getElementType();
		int elementTypeId = sharedTypes.get(elementType);
		
		if(elementTypeId == -1){
			out.write(SET_HEADER);
			
			doWriteType(elementType);
			
			sharedTypes.store(elementType);
		}else{
			out.write(SET_HEADER | TYPE_SHARED_FLAG);
			
			printInteger(elementTypeId);
		}

		printInteger(set.size());
		
		Iterator<IValue> content = set.iterator();
		while(content.hasNext()){
			doSerialize(content.next());
		}
	}
	
	private void writeRelation(IRelation relation) throws IOException{
		Type elementType = relation.getElementType();
		int elementTypeId = sharedTypes.get(elementType);
		
		if(elementTypeId == -1){
			out.write(RELATION_HEADER);
			
			doWriteType(elementType);
			
			sharedTypes.store(elementType);
		}else{
			out.write(RELATION_HEADER | TYPE_SHARED_FLAG);
			
			printInteger(elementTypeId);
		}
		
		printInteger(relation.size());
		
		Iterator<IValue> content = relation.iterator();
		while(content.hasNext()){
			doSerialize(content.next());
		}
	}
	
	private void writeMap(IMap map) throws IOException{
		Type mapType = map.getType();
		int mapTypeId = sharedTypes.get(mapType);
		
		if(mapTypeId == -1){
			out.write(MAP_HEADER);
			
			doWriteType(mapType);
			
			sharedTypes.store(mapType);
		}else{
			out.write(MAP_HEADER | TYPE_SHARED_FLAG);
			
			printInteger(mapTypeId);
		}
		
		printInteger(map.size());
		
		Iterator<Map.Entry<IValue, IValue>> content = map.entryIterator();
		while(content.hasNext()){
			Map.Entry<IValue, IValue> entry = content.next();
			
			doSerialize(entry.getKey());
			doSerialize(entry.getValue());
		}
	}
	
	private void writeValueType() throws IOException{
		out.write(VALUE_TYPE_HEADER);
	}
	
	private void writeVoidType() throws IOException{
		out.write(VOID_TYPE_HEADER);
	}
	
	private void writeBoolType() throws IOException{
		out.write(BOOL_TYPE_HEADER);
	}
	
	private void writeIntegerType() throws IOException{
		out.write(INTEGER_TYPE_HEADER);
	}
	
	private void writeRationalType() throws IOException{
		out.write(RATIONAL_TYPE_HEADER);
	}
	
	private void writeDoubleType() throws IOException{
		out.write(DOUBLE_TYPE_HEADER);
	}
	
	private void writeStringType() throws IOException{
		out.write(STRING_TYPE_HEADER);
	}
	
	private void writeSourceLocationType() throws IOException{
		out.write(SOURCE_LOCATION_TYPE_HEADER);
	}
	
	private void writeDateTimeType(Type dateTimeType) throws IOException{
		out.write(DATE_TIME_TYPE_HEADER);
	}
	
	private void writeNodeType(Type nodeType) throws IOException{
		Map<String, Type> declaredAnnotations = typeStore.getAnnotations(nodeType);
		if(declaredAnnotations.isEmpty()){
			out.write(NODE_TYPE_HEADER);
		}else{
			out.write(ANNOTATED_NODE_TYPE_HEADER);
			
			// Annotations.
			int nrOfAnnotations = declaredAnnotations.size();
			printInteger(nrOfAnnotations);
			
			Iterator<Map.Entry<String, Type>> declaredAnnotationsIterator = declaredAnnotations.entrySet().iterator();
			while(declaredAnnotationsIterator.hasNext()){
				Map.Entry<String, Type> declaredAnnotation = declaredAnnotationsIterator.next();
				
				String label = declaredAnnotation.getKey();
				byte[] labelBytes = label.getBytes(CharEncoding);
				printInteger(labelBytes.length);
				out.write(labelBytes);
				
				writeType(declaredAnnotation.getValue());
			}
		}
	}
	
	private void writeTupleType(Type tupleType) throws IOException{
		boolean hasFieldNames = tupleType.hasFieldNames();
		
		if(hasFieldNames){
			out.write(TUPLE_TYPE_HEADER | HAS_FIELD_NAMES);
			
			int arity = tupleType.getArity();
			printInteger(arity);
			for(int i = 0; i < arity; i++){
				writeType(tupleType.getFieldType(i));
				
				String name = tupleType.getFieldName(i);
				byte[] nameData = name.getBytes(CharEncoding);
				printInteger(nameData.length);
				out.write(nameData);
			}
		}else{
			out.write(TUPLE_TYPE_HEADER);
			
			int arity = tupleType.getArity();
			printInteger(arity);
			for(int i = 0; i < arity; i++){
				writeType(tupleType.getFieldType(i));
			}
		}
	}
	
	private void writeListType(Type listType) throws IOException{
		out.write(LIST_TYPE_HEADER);
		
		writeType(listType.getElementType());
	}
	
	private void writeSetType(Type setType) throws IOException{
		out.write(SET_TYPE_HEADER);
		
		writeType(setType.getElementType());
	}
	
	private void writeRelationType(Type relationType) throws IOException{
		out.write(RELATION_TYPE_HEADER);
		
		writeType(relationType.getElementType());
	}
	
	private void writeMapType(Type mapType) throws IOException{
		out.write(MAP_TYPE_HEADER);
		
		writeType(mapType.getKeyType());
		writeType(mapType.getValueType());
	}
	
	private void writeParameterType(Type parameterType) throws IOException{
		out.write(PARAMETER_TYPE_HEADER);
		
		String name = parameterType.getName();
		byte[] nameData = name.getBytes(CharEncoding);
		printInteger(nameData.length);
		out.write(nameData);
		
		writeType(parameterType.getBound());
	}
	
	private void writeADTType(Type adtType) throws IOException{
		out.write(ADT_TYPE_HEADER);
		
		String name = adtType.getName();
		byte[] nameData = name.getBytes(CharEncoding);
		printInteger(nameData.length);
		out.write(nameData);
		
		writeType(adtType.getTypeParameters());
	}
	
	private void writeConstructorType(Type constructorType) throws IOException{
		Map<String, Type> declaredAnnotations = typeStore.getAnnotations(constructorType);
		if(declaredAnnotations.isEmpty()){
			out.write(CONSTRUCTOR_TYPE_HEADER);
			
			String name = constructorType.getName();
			byte[] nameData = name.getBytes(CharEncoding);
			printInteger(nameData.length);
			out.write(nameData);
			
			writeType(constructorType.getFieldTypes());
			
			writeType(constructorType.getAbstractDataType());
		}else{
			out.write(ANNOTATED_CONSTRUCTOR_TYPE_HEADER);
			
			String name = constructorType.getName();
			byte[] nameData = name.getBytes(CharEncoding);
			printInteger(nameData.length);
			out.write(nameData);
			
			writeType(constructorType.getFieldTypes());
			
			writeType(constructorType.getAbstractDataType());
			
			// Annotations.
			int nrOfAnnotations = declaredAnnotations.size();
			printInteger(nrOfAnnotations);
			
			Iterator<Map.Entry<String, Type>> declaredAnnotationsIterator = declaredAnnotations.entrySet().iterator();
			while(declaredAnnotationsIterator.hasNext()){
				Map.Entry<String, Type> declaredAnnotation = declaredAnnotationsIterator.next();
				
				String label = declaredAnnotation.getKey();
				byte[] labelBytes = label.getBytes(CharEncoding);
				printInteger(labelBytes.length);
				out.write(labelBytes);
				
				writeType(declaredAnnotation.getValue());
			}
		}
	}
	
	private void writeAliasType(Type aliasType) throws IOException{
		out.write(ALIAS_TYPE_HEADER);
		
		String name = aliasType.getName();
		byte[] nameData = name.getBytes(CharEncoding);
		printInteger(nameData.length);
		out.write(nameData);
		
		writeType(aliasType.getAliased());
		
		writeType(aliasType.getTypeParameters());
	}
	
	private final static int SEVENBITS = 0x0000007f;
	private final static int SIGNBIT = 0x00000080;
	
	private void printInteger(int value) throws IOException{
		int intValue = value;
		
		if((intValue & 0xffffff80) == 0){
			out.write((byte) (intValue & SEVENBITS));
			return;
		}
		out.write((byte) ((intValue & SEVENBITS) | SIGNBIT));
		
		if((intValue & 0xffffc000) == 0){
			out.write((byte) ((intValue >>> 7) & SEVENBITS));
			return;
		}
		out.write((byte) (((intValue >>> 7) & SEVENBITS) | SIGNBIT));
		
		if((intValue & 0xffe00000) == 0){
			out.write((byte) ((intValue >>> 14) & SEVENBITS));
			return;
		}
		out.write((byte) (((intValue >>> 14) & SEVENBITS) | SIGNBIT));
		
		if((intValue & 0xf0000000) == 0){
			out.write((byte) ((intValue >>> 21) & SEVENBITS));
			return;
		}
		out.write((byte) (((intValue >>> 21) & SEVENBITS) | SIGNBIT));
		
		out.write((byte) ((intValue >>> 28) & SEVENBITS));
	}
}
