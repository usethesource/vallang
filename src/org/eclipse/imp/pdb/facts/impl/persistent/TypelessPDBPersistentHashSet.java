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
package org.eclipse.imp.pdb.facts.impl.persistent;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.AbstractSet;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.util.ImmutableSet;

/*
 * Operates:
 * 		* without types
 * 		* with equals() instead of isEqual()
 */
public final class TypelessPDBPersistentHashSet extends AbstractSet {
		
	private final ImmutableSet<IValue> content;

	public TypelessPDBPersistentHashSet(ImmutableSet<IValue> content) {
		this.content = content;
	}

	@Override
	protected IValueFactory getValueFactory() {
		return ValueFactory.getInstance();
	}

	@Override
	public Type getType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		return content.isEmpty();
	}

	@Override
	public ISet insert(IValue value) {
//		final ImmutableSet<IValue> contentNew = 
//				content.__insertEquivalent(value, equivalenceComparator);
//
//		if (content == contentNew)
//			return this;
//
//		final AbstractTypeBag bagNew = elementTypeBag.increase(value.getType());
//		
//		return new TypelessPDBPersistentHashSet(bagNew, contentNew);
		
		return new TypelessPDBPersistentHashSet(content.__insert(value));
	}

	@Override
	public ISet delete(IValue value) {
//		final ImmutableSet<IValue> contentNew = 
//				content.__removeEquivalent(value, equivalenceComparator);
//
//		if (content == contentNew)
//			return this;
//
//		final AbstractTypeBag bagNew = elementTypeBag.decrease(value.getType());
//		
//		return new TypelessPDBPersistentHashSet(bagNew, contentNew);
		
		return new TypelessPDBPersistentHashSet(content.__remove(value));
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
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (other == null)
			return false;
		
		if (other instanceof TypelessPDBPersistentHashSet) {
			TypelessPDBPersistentHashSet that = (TypelessPDBPersistentHashSet) other;
			
			if (this.size() != that.size())
				return false;

			return content.equals(that.content);
		}
		
		return false;
	}
	
	@Override
	public boolean isEqual(IValue other) {
		throw new UnsupportedOperationException();
	}

//	@Override
//	public ISet union(ISet other) {
//		if (other == this)
//			return this;
//		if (other == null)
//			return this;
//
//		if (other instanceof TypelessPDBPersistentHashSet) {
//			TypelessPDBPersistentHashSet that = (TypelessPDBPersistentHashSet) other;
//
//			final ImmutableSet<IValue> one;
//			final ImmutableSet<IValue> two;
//			AbstractTypeBag bag;
//			final ISet def;
//			
//			if (that.size() >= this.size()) {
//				def = that;
//				one = that.content;
//				bag = that.elementTypeBag;
//				two = this.content;
//			} else {
//				def = this;
//				one = this.content;
//				bag = this.elementTypeBag;
//				two = that.content;
//			}
//
//			final TransientSet<IValue> tmp = one.asTransient();
//			boolean modified = false;
//
//			for (IValue key : two) {
//				if (tmp.__insertEquivalent(key, equivalenceComparator)) {
//					modified = true;
//					bag = bag.increase(key.getType());
//				}
//			}
//			
//			if (modified) {
//				return new TypelessPDBPersistentHashSet(bag, tmp.freeze());
//			}
//			return def;
//		} else {
//			return super.union(other);
//		}
//	}
//	
//	@Override
//	public ISet intersect(ISet other) {
//		if (other == this)
//			return this;
//		if (other == null)
//			return EMPTY;
//
//		if (other instanceof TypelessPDBPersistentHashSet) {
//			TypelessPDBPersistentHashSet that = (TypelessPDBPersistentHashSet) other;
//
//			final ImmutableSet<IValue> one;
//			final ImmutableSet<IValue> two;
//			AbstractTypeBag bag;
//			final ISet def;
//			
//			if (that.size() >= this.size()) {
//				def = this;
//				one = this.content;
//				bag = this.elementTypeBag;
//				two = that.content;
//			} else {
//				def = that;
//				one = that.content;
//				bag = that.elementTypeBag;
//				two = this.content;
//			}
//			
//			final TransientSet<IValue> tmp = one.asTransient();
//			boolean modified = false;
//
//			for (Iterator<IValue> it = tmp.iterator(); it.hasNext();) {
//				final IValue key = it.next();
//				if (!two.containsEquivalent(key, equivalenceComparator)) {
//					it.remove();
//					modified = true;
//					bag = bag.decrease(key.getType());
//				}
//			}
//			
//			if (modified) {
//				return new TypelessPDBPersistentHashSet(bag, tmp.freeze());
//			}
//			return def;
//		} else {
//			return super.intersect(other);
//		}
//	}
//
//	@Override
//	public ISet subtract(ISet other) {
//		if (other == this)
//			return EMPTY;
//		if (other == null)
//			return this;
//
//		if (other instanceof TypelessPDBPersistentHashSet) {
//			TypelessPDBPersistentHashSet that = (TypelessPDBPersistentHashSet) other;
//
//			final ImmutableSet<IValue> one;
//			final ImmutableSet<IValue> two;
//			AbstractTypeBag bag;
//			final ISet def;
//			
//			def = this;
//			one = this.content;
//			bag = this.elementTypeBag;
//			two = that.content;
//			
//			final TransientSet<IValue> tmp = one.asTransient();
//			boolean modified = false;
//
//			for (IValue key : two) {
//				if (tmp.__removeEquivalent(key, equivalenceComparator)) {
//					modified = true;
//					bag = bag.decrease(key.getType());
//				}
//			}
//
//			if (modified) {
//				return new TypelessPDBPersistentHashSet(bag, tmp.freeze());
//			}
//			return def;
//		} else {
//			return super.subtract(other);
//		}
//	}

	@Override
	public ISet product(ISet that) {
		// TODO Auto-generated method stub
		return super.product(that);
	}

	@Override
	public boolean isSubsetOf(ISet that) {
		// TODO Auto-generated method stub
		return super.isSubsetOf(that);
	}
		
}
