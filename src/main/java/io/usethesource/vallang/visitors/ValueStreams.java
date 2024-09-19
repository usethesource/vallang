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

import java.util.ArrayDeque;
import java.util.Deque;
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

public class ValueStreams  {

    /**
     * Depth-first bottom-up traversal, left-to-right
     */
    public static Stream<IValue> bottomup(IValue val) {
        return val.accept(new BottomUp());
    }

    /**
     * Depth-first top-down traversal, left-to-right
     */
    public static Stream<IValue> topdown(IValue val) {
        return val.accept(new TopDown());
    }

    /**
     * Breadth-first top-down traversal, left-to-right
     * @param val
     * @return
     */
    public static Stream<IValue> topdownbf(IValue val) {
        Deque<IValue> queue = new ArrayDeque<>();

        queue.add(val);

        return topdownbf(queue);
    }

    private static Stream<IValue> topdownbf(Deque<IValue> queue) {
        if (queue.isEmpty()) {
            return Stream.empty();
        }

        IValue val = queue.remove();

        val.accept(new QueueChildren(queue));

        return Stream.concat(Stream.of(val), topdownbf(queue));
    }

    /**
     * Breadth-first bottom-up traversal, right-to-left
     * @param val
     * @return
     */
    public static Stream<IValue> bottomupbf(IValue val) {
        Deque<IValue> queue = new ArrayDeque<>();
        Deque<IValue> stack = new ArrayDeque<>();
        queue.add(val);

        while (!queue.isEmpty()) {
            val = queue.remove();
            stack.push(val);
            val.accept(new QueueChildren(queue));
        }

        return stack.stream();
    }

    private abstract static class Single implements IValueVisitor<Stream<IValue>, RuntimeException> {
        @Override
        public Stream<IValue> visitNode(INode o) {
            return Stream.of(o);
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
            return Stream.of(o);
        }

        @Override
        public Stream<IValue> visitSet(ISet o)  {
            return Stream.of(o);
        }

        @Override
        public Stream<IValue> visitSourceLocation(ISourceLocation o)  {
            return Stream.of(o);
        }

        @Override
        public Stream<IValue> visitTuple(ITuple o)  {
            return Stream.of(o);
        }

        @Override
        public Stream<IValue> visitConstructor(IConstructor o)  {
            return Stream.of(o);
        }

        @Override
        public Stream<IValue> visitInteger(IInteger o)  {
            return Stream.of(o);
        }

        @Override
        public Stream<IValue> visitMap(IMap o)  {
            return Stream.of(o);
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

    private static class BottomUp extends Single {
        @Override
        public Stream<IValue> visitNode(INode o) {
            return Stream.concat(
                    StreamSupport.stream(o.getChildren().spliterator(), false).flatMap(c -> c.accept(this)),
                    Stream.of(o));
        }

        @Override
        public Stream<IValue> visitList(IList o)  {
            return Stream.concat(
                    o.stream().flatMap(c -> c.accept(this)),
                    Stream.of(o));
        }

        @Override
        public Stream<IValue> visitSet(ISet o)  {
            return Stream.concat(
                    o.stream().flatMap(c -> c.accept(this)),
                    Stream.of(o));
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
        public Stream<IValue> visitMap(IMap o)  {
            Iterable<Entry<IValue,IValue>> it = (Iterable<Entry<IValue,IValue>>) () -> o.entryIterator();

            return Stream.concat(
                    StreamSupport.stream(it.spliterator(), false).flatMap(e -> {
                        return Stream.of(e.getKey(), e.getValue()).flatMap(c -> c.accept(this));
                    }),
                    Stream.of(o));
        }
    }

    private static class TopDown extends Single {
        @Override
        public Stream<IValue> visitNode(INode o) {
            return Stream.concat(Stream.of(o),
                    StreamSupport.stream(o.getChildren().spliterator(), false).flatMap(c -> c.accept(this))
                    );
        }

        @Override
        public Stream<IValue> visitList(IList o)  {
            return Stream.concat(Stream.of(o),
                    o.stream().flatMap(c -> c.accept(this))
                    );
        }

        @Override
        public Stream<IValue> visitSet(ISet o)  {
            return Stream.concat(Stream.of(o),
                    o.stream().flatMap(c -> c.accept(this))
                    );
        }

        @Override
        public Stream<IValue> visitTuple(ITuple o)  {
            return Stream.concat(Stream.of(o),
                    StreamSupport.stream(o.spliterator(), false).flatMap(c -> c.accept(this))
                    );
        }

        @Override
        public Stream<IValue> visitConstructor(IConstructor o)  {
            return Stream.concat(Stream.of(o),
                    StreamSupport.stream(o.getChildren().spliterator(), false)
                    );
        }

        @Override
        public Stream<IValue> visitMap(IMap o)  {
            Iterable<Entry<IValue,IValue>> it = (Iterable<Entry<IValue,IValue>>) () -> o.entryIterator();

            return Stream.concat(Stream.of(o),
                    StreamSupport.stream(it.spliterator(), false).flatMap(e -> {
                        return Stream.of(e.getKey(), e.getValue()).flatMap(c -> c.accept(this));
                    })
                    );
        }
    }

    private static class QueueChildren extends NullVisitor<Void, RuntimeException> {
        private final Deque<IValue> queue;

        public QueueChildren(Deque<IValue> queue) {
            this.queue = queue;
        }

        @Override
        public Void visitList(IList o) throws RuntimeException {
            o.stream().forEach((e) -> queue.add(e));
            return null;
        }

        @Override
        public Void visitSet(ISet o) throws RuntimeException {
            o.stream().forEach((e) -> queue.add(e));
            return null;
        }

        public Void visitTuple(ITuple o) throws RuntimeException {
            StreamSupport.stream(o.spliterator(), false).forEach((e) -> queue.add(e));
            return null;
        }

        @Override
        public Void visitNode(INode o) throws RuntimeException {
            StreamSupport.stream(o.getChildren().spliterator(), false).forEach((e) -> queue.add(e));
            return null;
        }

        @Override
        public Void visitConstructor(IConstructor o) throws RuntimeException {
            StreamSupport.stream(o.getChildren().spliterator(), false).forEach((e) -> queue.add(e));
            return null;
        }

        @Override
        public Void visitMap(IMap o) throws RuntimeException {
            Iterable<Entry<IValue,IValue>> it = (Iterable<Entry<IValue,IValue>>) () -> o.entryIterator();

            StreamSupport.stream(it.spliterator(), false).forEach(e -> {
                queue.add(e.getKey());
                queue.add(e.getValue());
            });

            return null;
        }
    }
}
