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

import org.eclipse.imp.pdb.facts.IDouble;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IObject;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.ISourceRange;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITree;
import org.eclipse.imp.pdb.facts.ITuple;

/**
 * This abstract class does nothing except implementing identity. Extend it
 * to easily implement a visitor that visits selected types of IValues.
 * 
 */
public abstract class NullVisitor<T> implements IValueVisitor<T> {
	public T visitDouble(IDouble o)  throws VisitorException{
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

	public <U> T visitObject(IObject<U> o)  throws VisitorException{
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

	public T visitSourceRange(ISourceRange o)  throws VisitorException{
		return null;
	}

	public T visitString(IString o)  throws VisitorException{
		return null;
	}

	public T visitTree(ITree o)  throws VisitorException{
		return null;
	}

	public T visitTuple(ITuple o)  throws VisitorException{
		return null;
	}
}
