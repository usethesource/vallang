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
package org.eclipse.imp.pdb.facts.impl.fast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashMap;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesHashSet;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;

/**
 * Implementation of IValueFactory.
 * 
 * @author Arnold Lankamp
 */
public final class ValueFactory implements IValueFactory{
	private final static TypeFactory tf = TypeFactory.getInstance();
	
	private final static String INTEGER_MAX_STRING = "2147483647";
	private final static String NEGATIVE_INTEGER_MAX_STRING = "-2147483648";
	
	private ValueFactory(){
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
	
	public IInteger integer(String integerValue){
		if(integerValue.startsWith("-")){
			if(integerValue.length() < 11 || (integerValue.length() == 11 && integerValue.compareTo(NEGATIVE_INTEGER_MAX_STRING) <= 0)){
				return new IntegerValue(Integer.parseInt(integerValue));
			}
			return new BigIntegerValue(new BigInteger(integerValue));
		}else{
			if(integerValue.length() < 10 || (integerValue.length() == 10 && integerValue.compareTo(INTEGER_MAX_STRING) <= 0)){
				return new IntegerValue(Integer.parseInt(integerValue));
			}
			return new BigIntegerValue(new BigInteger(integerValue));
		}
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
		return new BigIntegerValue(value);
	}
	
	public IReal real(double value){
		return new BigDecimalValue(new BigDecimal(value));
	}
	
	public IReal real(String doubleValue){
		return new BigDecimalValue(new BigDecimal(doubleValue));
	}
	
	public IReal real(BigDecimal value){
		return new BigDecimalValue(value);
	}
	
	public IString string(String value){
		return new StringValue(value);
	}
	
	public ISourceLocation sourceLocation(URL url, int offset, int length, int beginLine, int endLine, int beginCol, int endCol){
		return new SourceLocationValue(url, offset, length, beginLine, endLine, beginCol, endCol);
	}
	
	public IListWriter listWriter(Type elementType){
		return new ListWriter(elementType);
	}
	
	protected IListWriter createListWriter(Type elementType, ShareableValuesList data){
		return new ListWriter(elementType, data);
	}
	
	public IMapWriter mapWriter(Type keyType, Type valueType){
		return new MapWriter(keyType, valueType);
	}
	
	protected IMapWriter createMapWriter(Type keyType, Type valueType, ShareableValuesHashMap data){
		return new MapWriter(keyType, valueType, data);
	}
	
	public ISetWriter setWriter(Type elementType){
		if(elementType.isTupleType()) return relationWriter(elementType);
		
		return new SetWriter(elementType);
	}
	
	protected ISetWriter createSetWriter(Type elementType, ShareableValuesHashSet data){
		if(elementType.isTupleType()) return createRelationWriter(elementType, data);
		
		return new SetWriter(elementType, data);
	}
	
	public IRelationWriter relationWriter(Type tupleType){
		return new RelationWriter(tupleType);
	}
	
	protected IRelationWriter createRelationWriter(Type tupleType, ShareableValuesHashSet data){
		return new RelationWriter(tupleType, data);
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
	
	public INode node(String name, IValue... children){
		IValue[] copyOfChildren = new IValue[children.length];
		System.arraycopy(children, 0, copyOfChildren, 0, children.length);
		
		return new Node(name, copyOfChildren);
	}
	
	protected INode createNodeUnsafe(String name, IValue[] children){
		return new Node(name, children);
	}
	
	protected INode createAnnotatedNodeUnsafe(String name, IValue[] children, ShareableHashMap<String, IValue> annotations){
		return new AnnotatedNode(name, children, annotations);
	}
	
	public IConstructor constructor(Type constructorType){
		return new Constructor(constructorType, new IValue[0]);
	}
	
	public IConstructor constructor(Type constructorType, IValue... children){
		IValue[] copyOfChildren = new IValue[children.length];
		System.arraycopy(children, 0, copyOfChildren, 0, children.length);

		Type instantiatedType;
		if(!constructorType.getAbstractDataType().isParameterized()){
			instantiatedType = constructorType;
		}else{
			ShareableHashMap<Type, Type> bindings = new ShareableHashMap<Type,Type>();
			TypeFactory tf = TypeFactory.getInstance();
	
			constructorType.getFieldTypes().match(tf.tupleType(children), bindings);
			instantiatedType = constructorType.instantiate(new TypeStore(), bindings);
		}
		
		return new Constructor(instantiatedType, copyOfChildren);
	}
	
	protected IConstructor createConstructorUnsafe(Type constructorType, IValue[] children){
		return new Constructor(constructorType, children);
	}
	
	protected IConstructor createAnnotatedConstructorUnsafe(Type constructorType, IValue[] children, ShareableHashMap<String, IValue> annotations){
		return new AnnotatedConstructor(constructorType, children, annotations);
	}
	
	public ITuple tuple(){
		return new Tuple(new IValue[0]);
	}
	
	public ITuple tuple(IValue... args){
		IValue[] copyOfArgs = new IValue[args.length];
		System.arraycopy(args, 0, copyOfArgs, 0, args.length);
		return new Tuple(copyOfArgs);
	}
	
	protected ITuple createTupleUnsafe(IValue[] args){
		return new Tuple(args);
	}
	
	private static Type lub(IValue... elements) {
		Type elementType = TypeFactory.getInstance().voidType();
		
		for(int i = elements.length - 1; i >= 0; i--){
			elementType = elementType.lub(elements[i].getType());
		}
		
		return elementType;
	}
}
