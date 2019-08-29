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

import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
 * This visitor streams all sub-values of a value in a bottom-up fashion.
 *
 */
public class BottomUpStreamer implements IValueVisitor<Stream<IValue>, RuntimeException> {

    public static Stream<IValue> stream(IValue val) {
        return val.accept(new BottomUpStreamer());
    }
    
	@Override
	public Stream<IValue> visitNode(INode o) {
	    return Stream.concat(
	            StreamSupport.stream(o.getChildren().spliterator(), false).flatMap(c -> c.accept(this)),
	            Stream.of(o));
	}

    @Override
    public Stream<IValue> visitString(IString o)  {
        return Stream.of(o);
    }

    @Override
    public Stream<IValue> visitReal(IReal o)  {
        return Stream.of(o);
    }

    @Override
    public Stream<IValue> visitRational(IRational o)  {
        return Stream.of(o);
    }

    @Override
    public Stream<IValue> visitList(IList o)  {
        return Stream.concat(
                StreamSupport.stream(o.spliterator(), false).flatMap(c -> c.accept(this)),
                Stream.of(o));
    }

    @Override
    public Stream<IValue> visitRelation(ISet o)  {
        return visitSet(o);
    }

    @Override
    public Stream<IValue> visitListRelation(IList o)  {
        return visitList(o);
    }

    @Override
    public Stream<IValue> visitSet(ISet o)  {
        return Stream.concat(
                StreamSupport.stream(o.spliterator(), false).flatMap(c -> c.accept(this)),
                Stream.of(o));
    }

    @Override
    public Stream<IValue> visitSourceLocation(ISourceLocation o)  {
        return Stream.of(o);
    }

    @Override
    public Stream<IValue> visitTuple(ITuple o)  {
        return Stream.concat(
                StreamSupport.stream(o.spliterator(), false).flatMap(c -> c.accept(this)),
                Stream.of(o));
    }

    @Override
    public Stream<IValue> visitConstructor(IConstructor o)  {
        return Stream.concat(
                StreamSupport.stream(o.getChildren().spliterator(), false).flatMap(c -> c.accept(this)),
                Stream.of(o));
    }

    @Override
    public Stream<IValue> visitInteger(IInteger o)  {
        return Stream.of(o);
    }

    @Override
    public Stream<IValue> visitMap(IMap o)  {
        Iterable<Entry<IValue,IValue>> it = (Iterable<Entry<IValue,IValue>>) () -> o.entryIterator();
        
        return Stream.concat(
                StreamSupport.stream(it.spliterator(), false).flatMap(e -> {
                    return Stream.of(e.getKey(), e.getValue());
                }).flatMap(c -> c.accept(this)),
                Stream.of(o));
    }

    @Override
    public Stream<IValue> visitBoolean(IBool o)  {
        return Stream.of(o);
    }

    @Override
    public Stream<IValue> visitExternal(IExternalValue o)  {
        return Stream.of(o);
    }

    @Override
    public Stream<IValue> visitDateTime(IDateTime o)  {
        return Stream.of(o);
    }
	
}