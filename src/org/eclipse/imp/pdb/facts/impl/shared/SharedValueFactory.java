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
package org.eclipse.imp.pdb.facts.impl.shared;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;

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
import org.eclipse.imp.pdb.facts.exceptions.FactParseError;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.impl.fast.BoolValue;
import org.eclipse.imp.pdb.facts.impl.fast.IntegerValue;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IndexedCache;
import org.eclipse.imp.pdb.facts.impl.util.sharing.ShareableValuesFactory;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;

/**
 * Implementation of IValueFactory which constructs shared values.
 * 
 * @author Arnold Lankamp
 */
public final class SharedValueFactory implements IValueFactory{
	private final static TypeFactory tf = TypeFactory.getInstance();
	
	private final static Type EMPTY_TUPLE_TYPE = TypeFactory.getInstance().tupleEmpty();
	
	private final static String INTEGER_MAX_STRING = "2147483647";
	private final static String NEGATIVE_INTEGER_MAX_STRING = "-2147483648";
	
	private final static int CACHED_POSITIVE_INTEGERS_RANGE = 256;
	private final static int CACHED_NEGATIVE_INTEGERS_RANGE = -256;
	private final IndexedCache<IntegerValue> positiveIntegersCache;
	private final IndexedCache<IntegerValue> negativeIntegersCache;
	
	private final ShareableValuesFactory<IShareable> sharedValuesFactory;
	private final ShareableValuesFactory<SharedList> sharedListsFactory;
	private final ShareableValuesFactory<SharedMap> sharedMapsFactory;
	private final ShareableValuesFactory<SharedSet> sharedSetsFactory;
	private final ShareableValuesFactory<SharedRelation> sharedRelationsFactory;
	private final ShareableValuesFactory<SharedTuple> sharedTuplesFactory;
	private final ShareableValuesFactory<SharedNode> sharedNodesFactory;
	private final ShareableValuesFactory<SharedAnnotatedNode> sharedAnnotatedNodesFactory;
	private final ShareableValuesFactory<SharedConstructor> sharedConstructorsFactory;
	private final ShareableValuesFactory<SharedAnnotatedConstructor> sharedAnnotatedConstrutorsFactory;
	
	private SharedValueFactory(){
		super();
		
		positiveIntegersCache = new IndexedCache<IntegerValue>(CACHED_POSITIVE_INTEGERS_RANGE);
		negativeIntegersCache = new IndexedCache<IntegerValue>(-CACHED_NEGATIVE_INTEGERS_RANGE);
		
		sharedValuesFactory = new ShareableValuesFactory<IShareable>();
		sharedListsFactory = new ShareableValuesFactory<SharedList>();
		sharedMapsFactory = new ShareableValuesFactory<SharedMap>();
		sharedSetsFactory = new ShareableValuesFactory<SharedSet>();
		sharedRelationsFactory = new ShareableValuesFactory<SharedRelation>();
		sharedTuplesFactory = new ShareableValuesFactory<SharedTuple>();
		sharedNodesFactory = new ShareableValuesFactory<SharedNode>();
		sharedAnnotatedNodesFactory = new ShareableValuesFactory<SharedAnnotatedNode>();
		sharedConstructorsFactory = new ShareableValuesFactory<SharedConstructor>();
		sharedAnnotatedConstrutorsFactory = new ShareableValuesFactory<SharedAnnotatedConstructor>();
	}

	private static class InstanceKeeper{
		public final static SharedValueFactory instance = new SharedValueFactory();
	}
	
	public static SharedValueFactory getInstance(){
		return InstanceKeeper.instance;
	}
	
	protected IShareable buildValue(IShareable shareable){
		return sharedValuesFactory.build(shareable);
	}
	
	protected SharedList buildList(SharedList sharedList){
		return sharedListsFactory.build(sharedList);
	}
	
	protected SharedMap buildMap(SharedMap sharedMap){
		return sharedMapsFactory.build(sharedMap);
	}
	
	protected SharedSet buildSet(SharedSet sharedSet){
		return sharedSetsFactory.build(sharedSet);
	}
	
	protected SharedRelation buildRelation(SharedRelation sharedRelation){
		return sharedRelationsFactory.build(sharedRelation);
	}
	
	protected SharedTuple buildTuple(SharedTuple sharedTuple){
		return sharedTuplesFactory.build(sharedTuple);
	}
	
	protected SharedNode buildNode(SharedNode sharedNode){
		return sharedNodesFactory.build(sharedNode);
	}
	
	protected SharedAnnotatedNode buildAnnotatedNode(SharedAnnotatedNode sharedAnnotatedNode){
		return sharedAnnotatedNodesFactory.build(sharedAnnotatedNode);
	}
	
	protected SharedConstructor buildConstructor(SharedConstructor sharedConstructor){
		return sharedConstructorsFactory.build(sharedConstructor);
	}
	
	protected SharedAnnotatedConstructor buildAnnotatedConstructor(SharedAnnotatedConstructor sharedAnnotatedConstructor){
		return sharedAnnotatedConstrutorsFactory.build(sharedAnnotatedConstructor);
	}
	
	public IBool bool(boolean value){
		return BoolValue.getBoolValue(value); // NOTE: We don't have to share this; since the returned values are constants.
	}
	
	public IInteger integer(int value){
		// Check the caches, if the integer is within a certain range.
		if(value >= 0){
			if(value < CACHED_POSITIVE_INTEGERS_RANGE){
				IInteger cachedValue = positiveIntegersCache.get(value);
				if(cachedValue != null) return cachedValue;
				return positiveIntegersCache.getOrDefine(value, new SharedIntegerValue(value));
			}
		}else{
			if(value >= CACHED_NEGATIVE_INTEGERS_RANGE){
				int location = value - CACHED_NEGATIVE_INTEGERS_RANGE;
				IInteger cachedValue = negativeIntegersCache.get(location);
				if(cachedValue != null) return cachedValue;
				return negativeIntegersCache.getOrDefine(location, new SharedIntegerValue(value));
			}
		}
		
		// Otherwise, check the table.
		return (IInteger) buildValue(new SharedIntegerValue(value));
	}
	
	public IInteger integer(String integerValue){
		if(integerValue.startsWith("-")){
			if(integerValue.length() < 11 || (integerValue.length() == 11 && integerValue.compareTo(NEGATIVE_INTEGER_MAX_STRING) <= 0)){
				return integer(Integer.parseInt(integerValue));
			}
			return integer(new BigInteger(integerValue));
		}
		
		if(integerValue.length() < 10 || (integerValue.length() == 10 && integerValue.compareTo(INTEGER_MAX_STRING) <= 0)){
			return integer(Integer.parseInt(integerValue));
		}
		return integer(new BigInteger(integerValue));
	}
	
	public IInteger integer(byte[] integerData){
		if(integerData.length <= 4){
			int value = 0;
			for(int i = integerData.length - 1, j = 0; i >= 0; i--, j++){
				value |= ((integerData[i] & 0xff) << (j * 8));
			}
			
			return integer(value);
		}
		return integer(new BigInteger(integerData));
	}
	
	public IInteger integer(BigInteger bigInteger){
		return (IInteger) buildValue(new SharedBigIntegerValue(bigInteger));
	}
	
	public IReal real(double value){
		return (IReal) buildValue(new SharedBigDecimalValue(new BigDecimal(value)));
	}
	
	public IReal real(String doubleValue){
		return (IReal) buildValue(new SharedBigDecimalValue(new BigDecimal(doubleValue)));
	}
	
	public IReal real(BigDecimal value){
		return (IReal) buildValue(new SharedBigDecimalValue(value));
	}
	
	public IString string(String value){
		return (IString) buildValue(new SharedStringValue(value));
	}
	
	public ISourceLocation sourceLocation(URI url, int startOffset, int length, int startLine, int endLine, int startCol, int endCol){
		return (ISourceLocation) buildValue(new SharedSourceLocationValue(url, startOffset, length, startLine, endLine, startCol, endCol));
	}
	
	public ISourceLocation sourceLocation(String path, int startOffset, int length, int startLine, int endLine, int startCol, int endCol) {
    	try {
    		if (!path.startsWith("/")) {
    			path += "/";
    		}
			return sourceLocation(new URI("file://" + path), startOffset, length, startLine, endLine, startCol, endCol);
		} catch (URISyntaxException e) {
			throw new FactParseError("illegal path syntax", e);
		}
    }
	
	public ISourceLocation sourceLocation(URI url) {
		return sourceLocation(url, -1, -1, -1, -1, -1, -1);
	}
	
	public ISourceLocation sourceLocation(String path) {
		return sourceLocation(path, -1, -1, -1, -1, -1, -1);
	}
	
	public IListWriter listWriter(Type elementType){
		return new SharedListWriter(elementType);
	}
	
	public IMapWriter mapWriter(Type keyType, Type valueType){
		return new SharedMapWriter(keyType, valueType);
	}
	
	public ISetWriter setWriter(Type elementType){
		if(elementType.isTupleType()) return relationWriter(elementType);
		
		return new SharedSetWriter(elementType);
	}
	
	public IRelationWriter relationWriter(Type tupleType){
		return new SharedRelationWriter(tupleType);
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
		return buildNode(new SharedNode(name, new IValue[0]));
	}
	
	public INode node(String name, IValue... children){
		return buildNode(new SharedNode(name, children.clone()));
	}
	
	protected INode createNodeUnsafe(String name, IValue[] children){
		return buildNode(new SharedNode(name, children));
	}
	
	protected INode createAnnotatedNodeUnsafe(String name, IValue[] children, ShareableHashMap<String, IValue> annotations){
		return buildAnnotatedNode(new SharedAnnotatedNode(name, children, annotations));
	}
	
	public IConstructor constructor(Type constructorType){
		return buildConstructor(new SharedConstructor(constructorType, new IValue[0]));
	}
	
	public IConstructor constructor(Type constructorType, IValue... children){
		Type instantiatedType;
		if(!constructorType.getAbstractDataType().isParameterized()){
			instantiatedType = constructorType;
		}else{
			ShareableHashMap<Type, Type> bindings = new ShareableHashMap<Type,Type>();
			TypeFactory tf = TypeFactory.getInstance();
	
			constructorType.getFieldTypes().match(tf.tupleType(children), bindings);
			instantiatedType = constructorType.instantiate(new TypeStore(), bindings);
		}
		
		return buildConstructor(new SharedConstructor(instantiatedType, children.clone()));
	}
	
	protected IConstructor createConstructorUnsafe(Type constructorType, IValue[] children){
		return buildConstructor(new SharedConstructor(constructorType, children));
	}
	
	protected IConstructor createAnnotatedConstructorUnsafe(Type constructorType, IValue[] children, ShareableHashMap<String, IValue> annotations){
		return buildAnnotatedConstructor(new SharedAnnotatedConstructor(constructorType, children, annotations));
	}
	
	public ITuple tuple(){
		return buildTuple(new SharedTuple(EMPTY_TUPLE_TYPE, new IValue[0]));
	}
	
	public ITuple tuple(IValue... args){
		int nrOfArgs = args.length;
		Type[] elementTypes = new Type[nrOfArgs];
		for(int i = nrOfArgs - 1; i >= 0; i--){
			elementTypes[i] = args[i].getType();
		}
		
		return buildTuple(new SharedTuple(tf.tupleType(elementTypes), args.clone()));
	}
	
	protected ITuple createTupleUnsafe(Type tupleType, IValue[] args){
		return buildTuple(new SharedTuple(tupleType, args));
	}
	
	private static Type lub(IValue... elements){
		Type elementType = TypeFactory.getInstance().voidType();
		
		for(int i = elements.length - 1; i >= 0; i--){
			elementType = elementType.lub(elements[i].getType());
		}
		
		return elementType;
	}
}
