/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - interface and implementation
*    Arnold Lankamp - implementation
*    Anya Helene Bagge - rational support
*    Davy Landman - added PI & E constants
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.impl.BaseValueFactory;
import org.eclipse.imp.pdb.facts.impl.BoolValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;

/**
 * Implementation of IValueFactory.
 */
public class ValueFactory extends BaseValueFactory {
	private final static TypeFactory tf = TypeFactory.getInstance();
	
	private final static Type EMPTY_TUPLE_TYPE = TypeFactory.getInstance().tupleEmpty();
	
	private final static String INTEGER_MAX_STRING = "2147483647";
	private final static String NEGATIVE_INTEGER_MAX_STRING = "-2147483648";
	
	protected ValueFactory(){
		super();
	}

	private static class InstanceKeeper{
		public final static ValueFactory instance = new ValueFactory();
	}
	
	public static ValueFactory getInstance(){
		return InstanceKeeper.instance;
	}
	
	public IBool bool(boolean value){
		return BoolValue.getBoolValue(value);
	}
	
	public IInteger integer(int value){
		return new IntegerValue(value);
	}
	
	public IInteger integer(long value){
		if(((value & 0x000000007fffffffL) == value) || ((value & 0xffffffff80000000L) == 0xffffffff80000000L)){
			return integer((int) value);
		}else{
			byte[] valueData = new byte[8];
			valueData[0] = (byte) ((value >>> 56) & 0xff);
			valueData[1] = (byte) ((value >>> 48) & 0xff);
			valueData[2] = (byte) ((value >>> 40) & 0xff);
			valueData[3] = (byte) ((value >>> 32) & 0xff);
			valueData[4] = (byte) ((value >>> 24) & 0xff);
			valueData[5] = (byte) ((value >>> 16) & 0xff);
			valueData[6] = (byte) ((value >>> 8) & 0xff);
			valueData[7] = (byte) (value & 0xff);
			return integer(valueData);
		}
	}
	
	public IInteger integer(String integerValue){
		if(integerValue.startsWith("-")){
			if(integerValue.length() < 11 || (integerValue.length() == 11 && integerValue.compareTo(NEGATIVE_INTEGER_MAX_STRING) <= 0)){
				return new IntegerValue(Integer.parseInt(integerValue));
			}
			return new BigIntegerValue(new BigInteger(integerValue));
		}
		
		if(integerValue.length() < 10 || (integerValue.length() == 10 && integerValue.compareTo(INTEGER_MAX_STRING) <= 0)){
			return new IntegerValue(Integer.parseInt(integerValue));
		}
		return new BigIntegerValue(new BigInteger(integerValue));
	}
	
	public IInteger integer(byte[] integerData){
		if(integerData.length <= 4){
			int value = 0;
			for(int i = integerData.length - 1, j = 0; i >= 0; i--, j++){
				value |= ((integerData[i] & 0xff) << (j * 8));
			}
			
			return new IntegerValue(value);
		}
		return new BigIntegerValue(new BigInteger(integerData));
	}
	
	public IInteger integer(BigInteger value){
		if(value.bitLength() > 31){
			return new BigIntegerValue(value);
		}
		return new IntegerValue(value.intValue());
	}
	
	public IRational rational(int a, int b) {
		return rational(integer(a), integer(b));
	}

	public IRational rational(long a, long b) {
		return rational(integer(a), integer(b));
	}

	public IRational rational(IInteger a, IInteger b) {
		return new RationalValue(a, b);
	}

	public IRational rational(String rat) throws NumberFormatException {
		if(rat.contains("r")) {
			String[] parts = rat.split("r");
			if (parts.length == 2) {
				return rational(integer(parts[0]), integer(parts[1]));
			}
			if (parts.length == 1) {
				return rational(integer(parts[0]), integer(1));
			}
			throw new NumberFormatException(rat);
		}
		else {
			return rational(integer(rat), integer(1));
		}
	}

	@Override
	public IReal real(double value){
		return new BigDecimalValue(BigDecimal.valueOf(value));
	}
	
	@Override
	public IReal real(float value) {
		return new BigDecimalValue(BigDecimal.valueOf(value));
	}
	
	@Override
	public IReal real(String doubleValue){
		return new BigDecimalValue(new BigDecimal(doubleValue));
	}
	
	public IReal real(BigDecimal value){
		return new BigDecimalValue(value);
	}
	
	public IReal pi(int precision) {
		return BigDecimalValue.pi(precision);
	}
	
	public IReal e(int precision) {
		return BigDecimalValue.e(precision);
	}
	
	public IString string(String value){
		return new StringValue(value);
	}
	
	public IListWriter listWriter(Type elementType){
		return new ListWriter(elementType);
	}
	
	public IListWriter listWriter(){
		return new ListWriter();
	}
	
	public IMapWriter mapWriter(Type keyType, Type valueType){
		return new MapWriter(keyType, valueType);
	}
	
	public IMapWriter mapWriter(){
		return new MapWriter();
	}
	
	public ISetWriter setWriter(Type elementType){
		if(elementType.isTupleType()) return relationWriter(elementType);
		
		return new SetWriter(elementType);
	}
	
	public ISetWriter setWriter(){
		return new SetWriter();
	}
	
	public IRelationWriter relationWriter(Type tupleType){
		return new RelationWriter(tupleType);
	}
	
	public IRelationWriter relationWriter(){
		return new RelationWriter();
	}
	
	public IList list(Type elementType){
		return listWriter(elementType).done();
	}
	
	public IList list(IValue... elements){
		IListWriter listWriter = listWriter(lub(elements));
		listWriter.append(elements);
		
		return listWriter.done();
	}
	
	public IMap map(Type keyType, Type valueType){
		return mapWriter(keyType, valueType).done();
	}
	
	public ISet set(Type elementType){
		return setWriter(elementType).done();
	}
	
	public ISet set(IValue... elements){
		Type elementType = lub(elements);
		
		ISetWriter setWriter = setWriter(elementType);
		setWriter.insert(elements);
		return setWriter.done();
	}
	
	public IRelation relation(Type tupleType){
		return relationWriter(tupleType).done();
	}
	
	public IRelation relation(IValue... elements){
		Type elementType = lub(elements);
		
		if (!elementType.isTupleType()) throw new UnexpectedElementTypeException(tf.tupleType(tf.voidType()), elementType);
		
		IRelationWriter relationWriter = relationWriter(elementType);
		relationWriter.insert(elements);
		return relationWriter.done();
	}
	
	public INode node(String name){
		return new Node(name, new IValue[0]);
	}
	
	public INode node(String name, Map<String,IValue> annos, IValue... children){
		return new AnnotatedNode(name, children.clone(), annos);
	}
	
	public INode node(String name, IValue... children){
		return new Node(name, children.clone());
	}
	
	public IConstructor constructor(Type constructorType) {
		Type params = constructorType.getAbstractDataType().getTypeParameters();
		if (!params.isVoidType()) {
			ShareableHashMap<Type, Type> bindings = new ShareableHashMap<Type,Type>();
			for (Type p : params) {
				if (p.isParameterType()) {
					bindings.put(p, TypeFactory.getInstance().voidType());
				}
			}
			constructorType = constructorType.instantiate(bindings);
		}
		return new Constructor(constructorType, new IValue[0]);
	}
	
	public IConstructor constructor(Type constructorType, IValue... children){
		Type instantiatedType;
		if(!constructorType.getAbstractDataType().isParameterized()){
			instantiatedType = constructorType;
		}else{
			ShareableHashMap<Type, Type> bindings = new ShareableHashMap<Type,Type>();
			TypeFactory tf = TypeFactory.getInstance();
			Type params = constructorType.getAbstractDataType().getTypeParameters();
			for (Type p : params) {
				if (p.isParameterType()) {
					bindings.put(p, tf.voidType());
				}
			}
			constructorType.getFieldTypes().match(tf.tupleType(children), bindings);
			instantiatedType = constructorType.instantiate(bindings);
		}
		
		return new Constructor(instantiatedType, children.clone());
	}
	
	public IConstructor constructor(Type constructorType,
			Map<String, IValue> annotations, IValue... children)
			throws FactTypeUseException {
		Type instantiatedType;
		if(!constructorType.getAbstractDataType().isParameterized()){
			instantiatedType = constructorType;
		}else{
			ShareableHashMap<Type, Type> bindings = new ShareableHashMap<Type,Type>();
			TypeFactory tf = TypeFactory.getInstance();
			Type params = constructorType.getAbstractDataType().getTypeParameters();
			for (Type p : params) {
				if (p.isParameterType()) {
					bindings.put(p, tf.voidType());
				}
			}
			constructorType.getFieldTypes().match(tf.tupleType(children), bindings);
			instantiatedType = constructorType.instantiate(bindings);
		}
		
		ShareableHashMap<String, IValue> sAnnotations = new ShareableHashMap<String, IValue>();
		sAnnotations.putAll(annotations);
		
		return AnnotatedConstructor.createAnnotatedConstructor(instantiatedType, children.clone(), sAnnotations);
	}
	
	public ITuple tuple(){
		return new Tuple(EMPTY_TUPLE_TYPE, new IValue[0]);
	}
	
	public ITuple tuple(IValue... args){
		int nrOfArgs = args.length;
		Type[] elementTypes = new Type[nrOfArgs];
		for(int i = nrOfArgs - 1; i >= 0; i--){
			elementTypes[i] = args[i].getType();
		}
		
		return new Tuple(tf.tupleType(elementTypes), args.clone());
	}
	
	private static Type lub(IValue... elements) {
		Type elementType = TypeFactory.getInstance().voidType();
		
		for(int i = elements.length - 1; i >= 0; i--){
			elementType = elementType.lub(elements[i].getType());
		}
		
		return elementType;
	}

	public IString string(int[] chars) {
		StringBuilder b = new StringBuilder(chars.length);
		for (int ch : chars) {
			b.appendCodePoint(ch);
		}
		return string(b.toString());
	}

	public IString string(int ch) {
		StringBuilder b = new StringBuilder(1);
		b.appendCodePoint(ch);
		return string(b.toString());
	}
}
