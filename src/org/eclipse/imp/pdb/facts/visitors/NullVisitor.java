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
import org.eclipse.imp.pdb.facts.IExternalValue;
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
 * This abstract class does nothing except returning null. Extend it
 * to easily implement a visitor that visits selected types of IValues.
 * 
 */
public abstract class NullVisitor<T> implements IValueVisitor<T> {
	public T visitReal(IReal o)  throws VisitorException{
		return null;
	}

	public T visitInteger(IInteger o)  throws VisitorException{
		return null;
	}

	public T visitList(IList o)  throws VisitorException{
		return null;
	}

	public T visitMap(IMap o)  throws VisitorException{
		return null;
	}

	public T visitRelation(IRelation o)  throws VisitorException{
		return null;
	}

	public T visitSet(ISet o)  throws VisitorException{
		return null;
	}

	public T visitSourceLocation(ISourceLocation o)  throws VisitorException{
		return null;
	}

	public T visitString(IString o)  throws VisitorException{
		return null;
	}

	public T visitNode(INode o)  throws VisitorException{
		return null;
	}

	public T visitConstructor(IConstructor o) throws VisitorException {
		return null;
	}
	
	public T visitTuple(ITuple o)  throws VisitorException{
		return null;
	}
	
	public T visitBoolean(IBool boolValue) throws VisitorException {
		return null;
	}
	
	public T visitExternal(IExternalValue externalValue) {
		return null;
	}
}
