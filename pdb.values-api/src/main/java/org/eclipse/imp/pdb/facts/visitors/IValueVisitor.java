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

public interface IValueVisitor<R,E extends Throwable>  {
   public R visitString(IString o) throws E;
   public R visitReal(IReal o) throws E;
   public R visitRational(IRational o) throws E;
   public R visitList(IList o) throws E;
   public R visitRelation(ISet o) throws E;
   public R visitListRelation(IList o) throws E;
   public R visitSet(ISet o) throws E;
   public R visitSourceLocation(ISourceLocation o) throws E;
   public R visitTuple(ITuple o) throws E;
   public R visitNode(INode o) throws E;
   public R visitConstructor(IConstructor o) throws E;
   public R visitInteger(IInteger o) throws E;
   public R visitMap(IMap o) throws E;
   public R visitBoolean(IBool boolValue) throws E;
   public R visitExternal(IExternalValue externalValue) throws E;
   public R visitDateTime(IDateTime o) throws E;
}
