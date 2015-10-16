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

package io.usethesource.treasure.visitors;

import io.usethesource.treasure.IBool;
import io.usethesource.treasure.IConstructor;
import io.usethesource.treasure.IDateTime;
import io.usethesource.treasure.IExternalValue;
import io.usethesource.treasure.IInteger;
import io.usethesource.treasure.IList;
import io.usethesource.treasure.IMap;
import io.usethesource.treasure.INode;
import io.usethesource.treasure.IRational;
import io.usethesource.treasure.IReal;
import io.usethesource.treasure.ISet;
import io.usethesource.treasure.ISourceLocation;
import io.usethesource.treasure.IString;
import io.usethesource.treasure.ITuple;

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
