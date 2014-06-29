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
package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IAnnotatable;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.IWithKeywordParameters;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.func.NodeFunctions;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableMap;
import org.eclipse.imp.pdb.facts.util.ImmutableMap;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

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
			protected INode wrap(INode content, ImmutableMap<String, IValue> annotations) {
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
	    protected INode wrap(INode content, ImmutableMap<String, IValue> parameters) {
	      return new NodeWithKeywordParametersFacade(content, parameters);
	    }
    };
	}

}
