/**
 * Copyright (c) 2016, Davy Landman, Centrum Wiskunde & Informatica (CWI)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package io.usethesource.vallang.io.binary.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;
import io.usethesource.capsule.Map;
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
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.impl.fields.AbstractDefaultWithKeywordParameters;
import io.usethesource.vallang.visitors.IValueVisitor;

public class StacklessStructuredVisitor {

    public static <E extends Throwable> void accept(IValue root, StructuredIValueVisitor<E> visit) throws E {
        Deque<VisitStep<E>> workList = new ArrayDeque<>();
        workList.push(new SingleIValueStep<>(root, StacklessStructuredVisitor::visitValue));
        while (!workList.isEmpty()) {
            VisitStep<E> current = workList.peek();
            assert current != null: "no concurrent modification to the worklist, so a peek after an !isEmpty should never return null";
            // note, CF doesn't know this, so we have to still check against null in this case
            if (current != null && current.hasSteps()) {
                current.step(workList, visit);
            }
            else {
                workList.pop();
            }
        }
    }

    @FunctionalInterface
    private interface NextStepConsumer<E extends Throwable> {
        void accept(IValue current, Deque<VisitStep<E>> worklist, StructuredIValueVisitor<E> visit) throws E;
    }

    private abstract static class VisitStep<E extends Throwable> {
        final NextStepConsumer<E> next;
        VisitStep(NextStepConsumer<E> next) {
            this.next = next;
        }
        abstract boolean hasSteps();
        abstract void step(Deque<VisitStep<E>> worklist, StructuredIValueVisitor<E> visit) throws E;
    }

    private static final class SingleIValueStep<E extends Throwable> extends VisitStep<E> {
        final IValue val;
        boolean completed = false;
        public SingleIValueStep(IValue val, NextStepConsumer<E> next) {
            super(next);
            this.val = val;
        }

        @Override
        boolean hasSteps() {
            return !completed;
        }

        @Override
        void step(Deque<VisitStep<E>> worklist, StructuredIValueVisitor<E> visit) throws E {
            next.accept(val, worklist, visit);
            completed = true;
        }
    }

    private static final class IteratingSteps<E extends Throwable> extends VisitStep<E> {
        final Iterator<IValue> values;
        public IteratingSteps(Iterator<IValue> values, NextStepConsumer<E> next) {
            super(next);
            this.values = values;
        }

        @Override
        boolean hasSteps() {
            return values.hasNext();
        }

        @Override
        void step(Deque<VisitStep<E>> worklist, StructuredIValueVisitor<E> visit) throws E {
            next.accept(values.next(), worklist, visit);
        }
    }

    private static <E extends Throwable> void visitValue(IValue current, Deque<VisitStep<E>> workList, StructuredIValueVisitor<E> visit) throws E {
        current.accept(new IValueVisitor<Void, E>() {

            @Override
            public Void visitList(IList lst) throws E {
                if (visit.enterList(lst, lst.length())) {
                    workList.push(new SingleIValueStep<>(lst, (l, w, v) -> {
                        v.leaveList(l);
                    }));
                    workList.push(new IteratingSteps<>(lst.iterator(), StacklessStructuredVisitor::visitValue));
                }
                return null;
            }


            @Override
            public Void visitSet(ISet set) throws E {
                if (visit.enterSet(set, set.size())) {
                    workList.push(new SingleIValueStep<>(set, (l, w, v) -> {
                        v.leaveSet(l);
                    }));
                    workList.push(new IteratingSteps<>(set.iterator(), StacklessStructuredVisitor::visitValue));
                }
                return null;
            }

            @Override
            public Void visitMap(IMap map) throws E {
                if (visit.enterMap(map, map.size())) {
                    workList.push(new SingleIValueStep<>(map, (l, w, v) -> {
                        v.leaveMap(l);
                    }));


                    // we have to iterate, key,value pairs at a time
                    workList.push(new IteratingSteps<>(new Iterator<IValue>() {
                        Iterator<Entry<IValue, IValue>> entries = map.entryIterator();
                        @Nullable Entry<IValue, IValue> currentEntry = null;
                        @Override
                        public boolean hasNext() {
                            return currentEntry != null || entries.hasNext();
                        }

                        @Override
                        public IValue next() {
                            Entry<IValue, IValue> entry = currentEntry;
                            if (entry != null) {
                                currentEntry = null;
                                // we already did the key, so now return the value
                                return entry.getValue();
                            }
                            else if (entries.hasNext()) {
                                entry = currentEntry = entries.next();
                                return entry.getKey();
                            }
                            else {
                                throw new NoSuchElementException();
                            }
                        }
                    }, StacklessStructuredVisitor::visitValue));

                }
                return null;
            }

            @Override
            public Void visitTuple(ITuple tuple) throws E {
                if (visit.enterTuple(tuple, tuple.arity())) {
                    workList.push(new SingleIValueStep<>(tuple, (l, w, v) -> {
                        v.leaveTuple(l);
                    }));

                    workList.push(new IteratingSteps<>(tuple.iterator(), StacklessStructuredVisitor::visitValue));
                }
                return null;
            }

            @Override
            public Void visitNode(INode node) throws E {
                // WARNING, cloned to visitConstructor, fix bugs there as well!
                if (visit.enterNode(node, node.arity())) {
                    workList.push(new SingleIValueStep<>(node, (l, w, v) -> {
                        v.leaveNode(l);
                    }));
                    if(node.mayHaveKeywordParameters()){
                        IWithKeywordParameters<? extends INode> withKW = node.asWithKeywordParameters();
                        if(withKW.hasParameters()){
                            assert withKW instanceof AbstractDefaultWithKeywordParameters;
                            @SuppressWarnings("unchecked")
                            AbstractDefaultWithKeywordParameters<INode> nodeKw = (AbstractDefaultWithKeywordParameters<INode>)(withKW);
                            pushKWPairs(node, nodeKw.internalGetParameters());
                            workList.push(new SingleIValueStep<>(node, (l, w, v) -> {
                                v.enterNodeKeywordParameters();
                            }));
                        }

                    }

                    workList.push(new IteratingSteps<>(node.iterator(), StacklessStructuredVisitor::visitValue));

                }
                return null;
            }


            private void pushKWPairs(IValue root, Map.Immutable<String, IValue> namedValues) {
                workList.push(new SingleIValueStep<>(root, (l,w,v) -> {
                    v.leaveNamedValue();
                }));

                String[] names = new String[namedValues.size()];
                int i = names.length;
                Iterator<Entry<String, IValue>> entryIterator = namedValues.entryIterator();
                while (entryIterator.hasNext()) {
                    Entry<String, IValue> param = entryIterator.next();
                    workList.push(new SingleIValueStep<>(param.getValue(), StacklessStructuredVisitor::visitValue));
                    i--;
                    names[i] = param.getKey();
                }
                assert i == 0;
                workList.push(new SingleIValueStep<>(root, (l,w,v) -> {
                    v.enterNamedValues(names, names.length);
                }));
            }

            @Override
            public Void visitConstructor(IConstructor constr) throws E {
                // WARNING, cloned from visitNode, fix bugs there as well!
                if (visit.enterConstructor(constr, constr.arity())) {
                    workList.push(new SingleIValueStep<>(constr, (l, w, v) -> {
                        v.leaveConstructor(l);
                    }));
                    if(constr.mayHaveKeywordParameters()){
                        IWithKeywordParameters<? extends IConstructor> withKW = constr.asWithKeywordParameters();
                        if(withKW.hasParameters()){
                            assert withKW instanceof AbstractDefaultWithKeywordParameters;
                            @SuppressWarnings("unchecked")
                            AbstractDefaultWithKeywordParameters<IConstructor> constrKw = (AbstractDefaultWithKeywordParameters<IConstructor>)(withKW);
                            pushKWPairs(constr, constrKw.internalGetParameters());
                            workList.push(new SingleIValueStep<>(constr, (l, w, v) -> {
                                v.enterConstructorKeywordParameters();
                            }));
                        }

                    }

                    workList.push(new IteratingSteps<>(constr.iterator(), StacklessStructuredVisitor::visitValue));
                }
                return null;
            }



            @Override
            public Void visitExternal(IExternalValue externalValue) throws E {
                throw new RuntimeException("External values not supported yet");
            }

            @Override
            public Void visitSourceLocation(ISourceLocation o) throws E {
                visit.visitSourceLocation(o);
                return null;
            }
            @Override
            public Void visitInteger(IInteger o) throws E {
                visit.visitInteger(o);
                return null;
            }
            @Override
            public Void visitBoolean(IBool boolValue) throws E {
                visit.visitBoolean(boolValue);
                return null;
            }
            @Override
            public Void visitDateTime(IDateTime o) throws E {
                visit.visitDateTime(o);
                return null;
            }

            @Override
            public Void visitString(IString o) throws E {
                visit.visitString(o);
                return null;
            }

            @Override
            public Void visitReal(IReal o) throws E {
                visit.visitReal(o);
                return null;
            }

            @Override
            public Void visitRational(IRational o) throws E {
                visit.visitRational(o);
                return null;
            }
        });
    }

}
