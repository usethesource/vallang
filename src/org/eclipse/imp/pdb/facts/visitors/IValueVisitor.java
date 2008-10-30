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

public interface IValueVisitor {
   public IString visitString(IString o) throws VisitorException;
   public IDouble visitDouble(IDouble o) throws VisitorException;
   public IList visitList(IList o) throws VisitorException;
   public <T> IObject<T> visitObject(IObject<T> o) throws VisitorException;
   public IRelation visitRelation(IRelation o) throws VisitorException;
   public ISet visitSet(ISet o) throws VisitorException;
   public ISourceLocation visitSourceLocation(ISourceLocation o) throws VisitorException;
   public ISourceRange visitSourceRange(ISourceRange o) throws VisitorException;
   public ITuple visitTuple(ITuple o) throws VisitorException;
   public ITree visitTree(ITree o) throws VisitorException;
   public IInteger visitInteger(IInteger o) throws VisitorException;
   public IMap visitMap(IMap o) throws VisitorException;
}
