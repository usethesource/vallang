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

package org.rascalmpl.value.visitors;

import org.rascalmpl.value.IBool;
import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.IDateTime;
import org.rascalmpl.value.IExternalValue;
import org.rascalmpl.value.IInteger;
import org.rascalmpl.value.IList;
import org.rascalmpl.value.IMap;
import org.rascalmpl.value.INode;
import org.rascalmpl.value.IRational;
import org.rascalmpl.value.IReal;
import org.rascalmpl.value.ISet;
import org.rascalmpl.value.ISourceLocation;
import org.rascalmpl.value.IString;
import org.rascalmpl.value.ITuple;

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
