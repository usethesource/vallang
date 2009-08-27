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
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;

public interface IValueVisitor<R> {
   public R visitString(IString o) throws VisitorException;
   public R visitReal(IReal o) throws VisitorException;
   public R visitList(IList o) throws VisitorException;
   public R visitRelation(IRelation o) throws VisitorException;
   public R visitSet(ISet o) throws VisitorException;
   public R visitSourceLocation(ISourceLocation o) throws VisitorException;
   public R visitTuple(ITuple o) throws VisitorException;
   public R visitNode(INode o) throws VisitorException;
   public R visitConstructor(IConstructor o) throws VisitorException;
   public R visitInteger(IInteger o) throws VisitorException;
   public R visitMap(IMap o) throws VisitorException;
   public R visitBoolean(IBool boolValue) throws VisitorException;
   public R visitExternal(IExternalValue externalValue);
}
