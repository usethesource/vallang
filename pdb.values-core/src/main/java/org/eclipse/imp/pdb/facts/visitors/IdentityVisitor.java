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
import org.eclipse.imp.pdb.facts.IExternalValue;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IRational;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;

/**
 * This abstract class does nothing except implementing identity. Extend it
 * to easily implement a visitor that visits selected types of IValues.
 * 
 */
public abstract class IdentityVisitor<E extends Throwable> implements IValueVisitor<IValue, E> {
	public IValue visitReal(IReal o)  throws E{
		return o;
	}

	public IValue visitInteger(IInteger o)  throws E{
		return o;
	}

	public IValue visitRational(IRational o)  throws E{
		return o;
	}

	public IValue visitList(IList o)  throws E{
		return o;
	}

	public IValue visitMap(IMap o)  throws E{
		return o;
	}

	public IValue visitRelation(ISet o)  throws E{
		return o;
	}
	
	public IValue visitListRelation(IList o)  throws E{
		return o;
	}

	public IValue visitSet(ISet o)  throws E{
		return o;
	}

	public IValue visitSourceLocation(ISourceLocation o)  throws E{
		return o;
	}

	public IValue visitString(IString o)  throws E{
		return o;
	}

	public IValue visitNode(INode o)  throws E{
		return o;
	}
	
	public IValue visitConstructor(IConstructor o) throws E {
		return o;
	}

	public IValue visitTuple(ITuple o)  throws E{
		return o;
	}
	
	public IValue visitBoolean(IBool o) throws E {
		return o;
	}
	
	public IValue visitExternal(IExternalValue o) throws E {
		return o;
	}
	
	public IValue visitDateTime(IDateTime o) throws E {
		return o;
	}
}
