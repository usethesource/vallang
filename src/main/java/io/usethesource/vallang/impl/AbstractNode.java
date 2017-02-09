/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 ******************************************************************************/
package io.usethesource.vallang.impl;

import io.usethesource.capsule.api.Map;
import io.usethesource.vallang.IAnnotatable;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.impl.func.NodeFunctions;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.visitors.IValueVisitor;

import io.usethesource.capsule.util.collection.AbstractSpecialisedImmutableMap;

public abstract class AbstractNode extends AbstractValue implements INode {

	protected static TypeFactory getTypeFactory() {
		return TypeFactory.getInstance();
	}

	protected abstract IValueFactory getValueFactory();


	@Override
	public INode replace(int first, int second, int end, IList repl) throws FactTypeUseException, IndexOutOfBoundsException {
		return NodeFunctions.replace(getValueFactory(), this, first, second, end, repl);
	}

	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
		return v.visitNode(this);
	}
	
	@Override
	public boolean isAnnotatable() {
		return true;
	}
	
	@Override
	public IAnnotatable<? extends INode> asAnnotatable() {
		return new AbstractDefaultAnnotatable<INode>(this) {
			@Override
			protected INode wrap(INode content, Map.Immutable<String, IValue> annotations) {
				return new AnnotatedNodeFacade(content, annotations);
			}
		};
	}
	
	@Override
	public boolean mayHaveKeywordParameters() {
	  return true;
	}
	
	@Override
	public IWithKeywordParameters<? extends INode> asWithKeywordParameters() {
	  return new AbstractDefaultWithKeywordParameters<INode>(this, AbstractSpecialisedImmutableMap.<String, IValue>mapOf()) {
	    @Override
	    protected INode wrap(INode content, Map.Immutable<String, IValue> parameters) {
	      return new NodeWithKeywordParametersFacade(content, parameters);
	    }
    };
	}

}
