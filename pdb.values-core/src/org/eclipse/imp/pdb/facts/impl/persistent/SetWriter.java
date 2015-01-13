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

import java.util.Comparator;

import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.util.AbstractTypeBag;
import org.eclipse.imp.pdb.facts.util.EqualityUtils;
import org.eclipse.imp.pdb.facts.util.TransientSet;
import org.eclipse.imp.pdb.facts.util.TrieSet;

class SetWriter implements ISetWriter {

	@SuppressWarnings("unchecked")
	private static final Comparator<Object> equivalenceComparator = EqualityUtils
					.getEquivalenceComparator();

	protected AbstractTypeBag elementTypeBag;
	protected final TransientSet<IValue> setContent;

	protected final boolean checkUpperBound;
	protected final Type upperBoundType;
	protected ISet constructedSet;

	SetWriter(Type upperBoundType) {
		super();

		this.checkUpperBound = true;
		this.upperBoundType = upperBoundType;

		elementTypeBag = AbstractTypeBag.of();
		setContent = TrieSet.transientOf();
		constructedSet = null;
	}

	SetWriter() {
		super();

		this.checkUpperBound = false;
		this.upperBoundType = null;

		elementTypeBag = AbstractTypeBag.of();
		setContent = TrieSet.transientOf();
		constructedSet = null;
	}

	private void put(IValue element) {
		final Type elementType = element.getType();

		if (checkUpperBound && !elementType.isSubtypeOf(upperBoundType)) {
			throw new UnexpectedElementTypeException(upperBoundType, elementType);
		}

		boolean result = setContent.__insertEquivalent(element, equivalenceComparator);		
		if (result) {
			elementTypeBag = elementTypeBag.increase(elementType);
		}
	}

	@Override
	public void insert(IValue... values) throws FactTypeUseException {
		checkMutation();

		for (IValue item : values) {
			put(item);
		}
	}

	@Override
	public void insertAll(Iterable<? extends IValue> collection) throws FactTypeUseException {
		checkMutation();

		for (IValue item : collection) {
			put(item);
		}
	}

	@Override
	public ISet done() {
		if (constructedSet == null) {
			constructedSet = new PDBPersistentHashSet(elementTypeBag, setContent.freeze());
		}

		return constructedSet;
	}

	private void checkMutation() {
		if (constructedSet != null) {
			throw new UnsupportedOperationException("Mutation of a finalized set is not supported.");
		}
	}

	@Override
	public String toString() {
		return setContent.toString();
	}

}
