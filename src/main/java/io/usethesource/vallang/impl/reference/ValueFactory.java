/*******************************************************************************
* Copyright (c) 2007, 2008, 2012 IBM Corporation & CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Jurgen Vinju (jurgen@vinju.org)
*    Anya Helene Bagge - rational support, labeled maps and tuples
*******************************************************************************/

package io.usethesource.vallang.impl.reference;

import java.util.Objects;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.impl.primitive.AbstractPrimitiveValueFactory;
import io.usethesource.vallang.type.Type;

/**
 * This is a reference implementation for an @{link IValueFactory}. It uses
 * the Java standard library to implement it in a most straightforward but
 * not necessarily very efficient manner.
 */
public class ValueFactory extends AbstractPrimitiveValueFactory {
	private static final ValueFactory sInstance = new ValueFactory();
	public static ValueFactory getInstance() {
		return sInstance;
	}

	protected ValueFactory() {
		super();
	}

	protected static void checkNull(Object... args) {
		for (Object a : args) {
			Objects.requireNonNull(a);
		}
	}

	protected static void checkNull(java.util.Map<Object, Object> args) {
		for (java.util.Map.Entry<Object, Object> entry : args.entrySet()) {
			if (entry == null || entry.getKey() == null || entry.getValue() == null) {
				throw new NullPointerException();
			}
		}
	}

	@Override
	public ISetWriter setWriter() {
		return new SetWriter();
	}

	@Override
	public ISet set(IValue... elems) throws FactTypeUseException {
		checkNull((Object[]) elems);
		
		ISetWriter sw = setWriter();
		sw.insert(elems);
		return sw.done();
	}

	@Override
	public IListWriter listWriter() {
		return new ListWriter();
	}

	@Override
	public IList list(IValue... rest) {
		checkNull((Object[]) rest);
		IListWriter lw =  listWriter();
		lw.append(rest);
		return lw.done();
	}

	@Override
	public ITuple tuple() {
		return new Tuple(new IValue[0]);
	}

	@Override
	public ITuple tuple(IValue... args) {
		checkNull((Object[]) args);

		return new Tuple(args.clone());
	}

	@Override
	public ITuple tuple(Type type, IValue... args) {
		checkNull((Object[]) args);

		return new Tuple(type, args.clone());
	}
	
	@Override
	public INode node(String name) {
		checkNull(name);
		return new Node(name);
	}
	
	@SuppressWarnings("deprecation")
    @Override
	public INode node(String name, java.util.Map<String, IValue> annotations, IValue... children) {
		checkNull(name);
		checkNull(annotations);
		checkNull((Object[]) children);
				
		return new Node(name, children).asAnnotatable().setAnnotations(annotations);
	}
	
	@Override
	public INode node(String name, IValue... children) {
		checkNull(name);
		checkNull((Object[]) children);
		return new Node(name, children);
	}
	
	@Override
	public INode node(String name,  IValue[] children, java.util.Map<String, IValue> keyArgValues)
			throws FactTypeUseException {
		checkNull(name);
		checkNull((Object[]) children);
//		checkNull(keyArgValues); // fails; are null values allowed?
		
		return new Node(name, children.clone()).asWithKeywordParameters().setParameters(keyArgValues);
	}
		
	@Override
	public IConstructor constructor(Type constructorType, IValue... children) {
		checkNull(constructorType);
		checkNull((Object[]) children);
		return new Constructor(constructorType, children);
	}
	
	@SuppressWarnings("deprecation")
    @Override
	public IConstructor constructor(Type constructorType, java.util.Map<String,IValue> annotations, IValue... children) {
		checkNull(constructorType);
		checkNull(annotations);
		checkNull((Object[]) children);
				
		return new Constructor(constructorType, children).asAnnotatable().setAnnotations(annotations);
	}
	
	@Override
	public IConstructor constructor(Type constructorType,  IValue[] children, java.util.Map<String,IValue> kwParams) {
	    checkNull(constructorType);
	    checkNull(kwParams);
	    checkNull((Object[]) children);

	    return new Constructor(constructorType, children).asWithKeywordParameters().setParameters(kwParams);
	}
	
	@Override
	public IConstructor constructor(Type constructorType) {
		checkNull(constructorType);
		return new Constructor(constructorType);
	}

	@Override
	public IMapWriter mapWriter() {
		return new MapWriter();
	}

	@Override
	public String toString() {
		return "VALLANG_REFERENCE_FACTORY";
	}
	
}
