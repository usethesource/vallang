/*******************************************************************************
 * Copyright (c) 2013-2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import java.util.Iterator;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.capsule.Set;
import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.util.AbstractTypeBag;

public final class PersistentHashSet implements ISet {
    private @Nullable Type cachedSetType = null;
    private final AbstractTypeBag elementTypeBag;
    private final Set.Immutable<IValue> content;

    /**
    * Construction of persistent hash-set.
    *
    * DO NOT CALL OUTSIDE OF {@link PersistentSetFactory}.
    *
    * @param elementTypeBag precise dynamic type
    * @param content immutable set
    */
    PersistentHashSet(AbstractTypeBag elementTypeBag, Set.Immutable<IValue> content) {
        this.elementTypeBag = Objects.requireNonNull(elementTypeBag);
        this.content = Objects.requireNonNull(content);

        assert checkDynamicType(elementTypeBag, content);
        assert !(elementTypeBag.lub() == TF.voidType() || content.isEmpty());
    }

    @Override
    public String toString() {
        return defaultToString();
    }

    private static final boolean checkDynamicType(final AbstractTypeBag elementTypeBag,
        final Set.Immutable<IValue> content) {

        final AbstractTypeBag expectedElementTypeBag =
            content.stream().map(IValue::getType).collect(AbstractTypeBag.toTypeBag());

        boolean expectedTypesEqual = expectedElementTypeBag.equals(elementTypeBag);

        return expectedTypesEqual;
    }

    @Override
    public ISetWriter writer() {
        return ValueFactory.getInstance().setWriter();
    }

    @Override
    public Type getType() {
        if (cachedSetType == null) {
            cachedSetType = TF.setType(elementTypeBag.lub());
        }

        return cachedSetType;
    }

    @Override
    public boolean isEmpty() {
        return content.isEmpty();
    }

    @Override
    public ISet insert(IValue value) {
        final Set.Immutable<IValue> contentNew =
            content.__insert(value);

        if (content == contentNew)
            return this;

        final AbstractTypeBag bagNew = elementTypeBag.increase(value.getType());

        return PersistentSetFactory.from(bagNew, contentNew);
    }

    @Override
    public ISet delete(IValue value) {
        final Set.Immutable<IValue> contentNew =
            content.__remove(value);

        if (content == contentNew)
            return this;

        final AbstractTypeBag bagNew = elementTypeBag.decrease(value.getType());

        return PersistentSetFactory.from(bagNew, contentNew);
    }

    @Override
    public int size() {
        return content.size();
    }

    @Override
    public boolean contains(IValue value) {
        return content.contains(value);
    }

    @Override
    public Iterator<IValue> iterator() {
        return content.iterator();
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }

        if (other == null) {
            return false;
        }

        if (other instanceof PersistentHashSet) {
            PersistentHashSet that = (PersistentHashSet) other;

            if (this.getType() != that.getType()) {
                return false;
            }

            if (this.size() != that.size()) {
                return false;
            }

            return content.equals(that.content);
        }

        if (other instanceof ISet) {
            return defaultEquals(other);
        }

        return false;
    }

    @Override
    public ISet union(ISet other) {
        if (other == this) {
            return this;
        }

        if (other instanceof PersistentHashSet) {
            PersistentHashSet that = (PersistentHashSet) other;

            final Set.Immutable<IValue> one;
            final Set.Immutable<IValue> two;
            AbstractTypeBag bag;
            final ISet def;

            if (that.size() >= this.size()) {
                def = that;
                one = that.content;
                bag = that.elementTypeBag;
                two = this.content;
            } else {
                def = this;
                one = this.content;
                bag = this.elementTypeBag;
                two = that.content;
            }

            final Set.Transient<IValue> tmp = one.asTransient();
            boolean modified = false;

            for (IValue key : two) {
                if (tmp.__insert(key)) {
                    modified = true;
                    bag = bag.increase(key.getType());
                }
            }

            if (modified) {
                return PersistentSetFactory.from(bag, tmp.freeze());
            }
            return def;
        } else {
            return ISet.super.union(other);
        }
    }

    @Override
    public ISet intersect(ISet other) {
        if (other == this) {
            return this;
        }

        if (other instanceof PersistentHashSet) {
            PersistentHashSet that = (PersistentHashSet) other;

            final Set.Immutable<IValue> one;
            final Set.Immutable<IValue> two;
            AbstractTypeBag bag;
            final ISet def;

            if (that.size() >= this.size()) {
                def = this;
                one = this.content;
                bag = this.elementTypeBag;
                two = that.content;
            } else {
                def = that;
                one = that.content;
                bag = that.elementTypeBag;
                two = this.content;
            }

            final Set.Transient<IValue> tmp = one.asTransient();
            boolean modified = false;

            for (Iterator<IValue> it = tmp.iterator(); it.hasNext();) {
                final IValue key = it.next();
                if (!two.contains(key)) {
                    it.remove();
                    modified = true;
                    bag = bag.decrease(key.getType());
                }
            }

            if (modified) {
                return PersistentSetFactory.from(bag, tmp.freeze());
            }
            return def;
        } else {
            return ISet.super.intersect(other);
        }
    }

    @Override
    public ISet subtract(ISet other) {
        if (other == this) {
            return EmptySet.EMPTY_SET;
        }

        if (other == null) {
            return this;
        }

        if (other instanceof PersistentHashSet) {
            PersistentHashSet that = (PersistentHashSet) other;

            final Set.Immutable<IValue> one;
            final Set.Immutable<IValue> two;
            AbstractTypeBag bag;
            final ISet def;

            def = this;
            one = this.content;
            bag = this.elementTypeBag;
            two = that.content;

            final Set.Transient<IValue> tmp = one.asTransient();
            boolean modified = false;

            for (IValue key : two) {
                if (tmp.__remove(key)) {
                    modified = true;
                    bag = bag.decrease(key.getType());
                }
            }

            if (modified) {
                return PersistentSetFactory.from(bag, tmp.freeze());
            }
            return def;
        } else {
            return ISet.super.subtract(other);
        }
    }

    @Override
    public IRelation<ISet> asRelation() {
        return new PersistentSetRelation(this);
    }
}
