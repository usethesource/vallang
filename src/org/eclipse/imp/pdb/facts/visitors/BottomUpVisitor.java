/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    jurgen@vinju.org

*******************************************************************************/
package org.eclipse.imp.pdb.facts.visitors;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ITree;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

/**
 * This visitor will apply another visitor in a bottom-up fashion to an IValue 
 *
 */
public class BottomUpVisitor extends VisitorAdapter {
	protected IValueFactory fFactory;

	public BottomUpVisitor(IValueVisitor visitor, IValueFactory factory) {
		super(visitor);
		this.fFactory = factory;
	}
	
	@Override
	public ITree visitTree(ITree o) throws VisitorException {
		for (int i = 0; i < o.arity(); i++) {
			o = o.set(i, o.get(i).accept(this));
		}
		
		return fVisitor.visitTree(o);
	}
	
	@Override
	public IList visitList(IList o) throws VisitorException {
		IList result = fFactory.list(o.getElementType());
		for (IValue elem : o) {
			result.append(elem.accept(this));
		}
		
		return fVisitor.visitList(result);
	}
	
	@Override
	public ISet visitSet(ISet o) throws VisitorException {
		ISet result = fFactory.set(o.getElementType());
		for (IValue elem : o) {
			result.insert(elem.accept(this));
		}
		
		return fVisitor.visitSet(result);
	}
	
	@Override
	public IMap visitMap(IMap o) throws VisitorException {
		IMap result = fFactory.map(o.getKeyType(), o.getValueType());
		for (IValue elem : o) {
			result.put(elem.accept(this), o.get(elem).accept(this));
		}
		
		return fVisitor.visitMap(result);
	}

	@Override
	public IRelation visitRelation(IRelation o) throws VisitorException {
		IRelation result = fFactory.relation(o.getFieldTypes());
		
		for (ITuple tuple : o) {
			result.insert((ITuple) tuple.accept(this));
		}
		
		return fVisitor.visitRelation(o);
	}
	
	@Override
	public ITuple visitTuple(ITuple o) throws VisitorException {
		for (int i = 0; i < o.arity(); i++) {
			o.set(i, o.get(i).accept(this));
		}
		
		return fVisitor.visitTuple(o);
	}
}
