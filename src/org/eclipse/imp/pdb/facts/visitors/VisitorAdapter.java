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

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IDateTime;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;

/**
 * Extend this class to easily create a reusable generic visitor implementation.
 *
 */
public abstract class VisitorAdapter<T> implements IValueVisitor<T> {
	protected IValueVisitor<T> fVisitor;

	public VisitorAdapter(IValueVisitor<T> visitor) {
		this.fVisitor = visitor;
	}

	public T visitReal(IReal o) throws VisitorException {
		return fVisitor.visitReal(o);
	}

	public T visitInteger(IInteger o) throws VisitorException {
		return fVisitor.visitInteger(o);
	}

	public T visitList(IList o) throws VisitorException {
		return fVisitor.visitList(o);
	}

	public T visitMap(IMap o) throws VisitorException {
		return fVisitor.visitMap(o);
	}

	public T visitRelation(IRelation o) throws VisitorException {
		return fVisitor.visitRelation(o);
	}

	public T visitSet(ISet o) throws VisitorException {
		return fVisitor.visitSet(o);
	}

	public T visitSourceLocation(ISourceLocation o) throws VisitorException {
		return fVisitor.visitSourceLocation(o);
	}

	public T visitString(IString o) throws VisitorException {
		return fVisitor.visitString(o);
	}

	public T visitNode(INode o) throws VisitorException {
		return fVisitor.visitNode(o);
	}

	public T visitConstructor(IConstructor o) throws VisitorException {
		return fVisitor.visitNode(o);
	}
	
	public T visitTuple(ITuple o) throws VisitorException {
		return fVisitor.visitTuple(o);
	}
	
	public T visitBoolean(IBool o) throws VisitorException {
		return fVisitor.visitBoolean(o);
	}
	
	public T visitDateTime(IDateTime o) throws VisitorException {
		return fVisitor.visitDateTime(o);
	}
}
