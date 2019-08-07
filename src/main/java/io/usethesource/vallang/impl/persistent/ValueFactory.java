/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import io.usethesource.vallang.impl.util.collections.ShareableValuesList;
import io.usethesource.vallang.type.TypeFactory;
import java.util.Map;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.impl.primitive.AbstractPrimitiveValueFactory;
import io.usethesource.vallang.type.Type;

public class ValueFactory extends AbstractPrimitiveValueFactory {

	protected ValueFactory() {
		super();
	}

	private static class InstanceKeeper {
		public final static ValueFactory instance = new ValueFactory();
	}

	public static ValueFactory getInstance() {
		return InstanceKeeper.instance;
	}

	@Override
    public IListWriter listWriter() {
        return new ListWriter();
    }

    private static final IList EMPTY_LIST = List.newList(TypeFactory.getInstance().voidType(), new ShareableValuesList());
    
    @Override
    public IList list(IValue... elements){
	    if (elements.length == 0) {
	        return EMPTY_LIST;
        }
        IListWriter listWriter = listWriter();
        listWriter.append(elements);
        
        return listWriter.done();
    }

    @Override
    public ISet set(IValue... elements){
        if (elements.length == 0) {
            return EmptySet.EMPTY_SET;
        }
        ISetWriter setWriter = setWriter();
        setWriter.insert(elements);
        return setWriter.done();
    }
    
    @Override
    public INode node(String name) {
        return Node.newNode(name, new IValue[0]);
    }

    @SuppressWarnings("deprecation")
    @Override
    public INode node(String name, Map<String, IValue> annos, IValue... children) {
        return Node.newNode(name, children.clone()).asAnnotatable().setAnnotations(annos);
    }

    @Override
    public INode node(String name, IValue... children) {
        return Node.newNode(name, children.clone());
    }
    
    @Override
    public INode node(String name, IValue[] children, Map<String, IValue> keyArgValues) {
        return Node.newNode(name, children.clone()).asWithKeywordParameters().setParameters(keyArgValues);
    }
    
    @Override
    public IConstructor constructor(Type constructorType) {
        return Constructor.newConstructor(constructorType, new IValue[0]);
    }
    
    @Override
    public IConstructor constructor(Type constructorType, IValue... children){
        return Constructor.newConstructor(constructorType, children.clone());
    }
    
    @Override
    public IConstructor constructor(Type constructorType, IValue[] children, Map<String,IValue> kwParams){
        return Constructor.newConstructor(constructorType, children.clone(), kwParams);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public IConstructor constructor(Type constructorType,
            Map<String, IValue> annotations, IValue... children) {
        return Constructor.newConstructor(constructorType, children.clone()).asAnnotatable().setAnnotations(annotations);
    }
    
    @Override
    public ITuple tuple() {
        return Tuple.newTuple();
    }

    @Override
    public ITuple tuple(IValue... args) {
        return Tuple.newTuple(args.clone());
    }

	@Override
	public ISetWriter setWriter() {
		return new SetWriter((a,b) -> tuple(a,b));
	}

	@Override
	public IMapWriter mapWriter() {
		return new MapWriter();
	}

	@Override
	public String toString() {
		return "VALLANG_PERSISTENT_FACTORY";
	}
}
