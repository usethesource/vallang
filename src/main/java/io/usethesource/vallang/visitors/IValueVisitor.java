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

import org.checkerframework.checker.nullness.qual.Nullable;

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

public interface IValueVisitor<R extends @Nullable Object, E extends Throwable>  {
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
