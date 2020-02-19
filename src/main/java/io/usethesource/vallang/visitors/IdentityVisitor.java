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
package io.usethesource.vallang.visitors;

import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.IExternalValue;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;

/**
 * This abstract class does nothing except implementing identity. Extend it
 * to easily implement a visitor that visits selected types of IValues.
 * 
 */
public abstract class IdentityVisitor<E extends Throwable> implements IValueVisitor<IValue, E> {
    @Override
	public IValue visitReal(IReal o)  throws E{
		return o;
	}

    @Override
	public IValue visitInteger(IInteger o)  throws E{
		return o;
	}

    @Override
	public IValue visitRational(IRational o)  throws E{
		return o;
	}

    @Override
	public IValue visitList(IList o)  throws E{
		return o;
	}

    @Override
	public IValue visitMap(IMap o)  throws E{
		return o;
	}

    @Override
	public IValue visitSet(ISet o)  throws E{
		return o;
	}

    @Override
	public IValue visitSourceLocation(ISourceLocation o)  throws E{
		return o;
	}

    @Override
	public IValue visitString(IString o)  throws E{
		return o;
	}

    @Override
	public IValue visitNode(INode o)  throws E{
		return o;
	}
	
    @Override
	public IValue visitConstructor(IConstructor o) throws E {
		return o;
	}

    @Override
	public IValue visitTuple(ITuple o)  throws E{
		return o;
	}
	
    @Override
	public IValue visitBoolean(IBool o) throws E {
		return o;
	}
	
    @Override
	public IValue visitExternal(IExternalValue o) throws E {
		return o;
	}
	
    @Override
	public IValue visitDateTime(IDateTime o) throws E {
		return o;
	}
}
