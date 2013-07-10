/*******************************************************************************
 * Copyright (c) 2013 CWI
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

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.func.NodeFunctions;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

public abstract class AbstractNode extends AbstractValue implements INode {

	protected static TypeFactory getTypeFactory() {
		return TypeFactory.getInstance();
	}

	protected abstract IValueFactory getValueFactory();

	@Override
	public boolean hasKeywordArguments() {
		return NodeFunctions.hasKeywordArguments(getValueFactory(), this);
	}

	@Override
	public int getKeywordIndex(String name) {
		return NodeFunctions.getKeywordIndex(getValueFactory(), this, name);
	}

	@Override
	public IValue getKeywordArgumentValue(String name) {
		return NodeFunctions.getKeywordArgumentValue(getValueFactory(), this, name);
	}

	@Override
	public int positionalArity() {
		return NodeFunctions.positionalArity(getValueFactory(), this);
	}

//	@Override
//	public IValue getAnnotation(String label) throws FactTypeUseException {
//		return null;  //To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public INode setAnnotation(String label, IValue newValue) throws FactTypeUseException {
//		return null;  //To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public boolean hasAnnotation(String label) throws FactTypeUseException {
//		return false;  //To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public boolean hasAnnotations() {
//		return false;  //To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public Map<String, IValue> getAnnotations() {
//		return null;  //To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public INode setAnnotations(Map<String, IValue> annotations) {
//		return null;  //To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public INode joinAnnotations(Map<String, IValue> annotations) {
//		return null;  //To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public INode removeAnnotation(String key) {
//		return null;  //To change body of implemented methods use File | Settings | File Templates.
//	}
//
//	@Override
//	public INode removeAnnotations() {
//		return null;  //To change body of implemented methods use File | Settings | File Templates.
//	}

	@Override
	public INode replace(int first, int second, int end, IList repl) throws FactTypeUseException, IndexOutOfBoundsException {
		return NodeFunctions.replace(getValueFactory(), this, first, second, end, repl);
	}

	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
		return v.visitNode(this);
	}

}
