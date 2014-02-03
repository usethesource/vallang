/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *
 * Based on code by:
 *
 *   * Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.persistent;

import java.util.Comparator;

import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.impl.AbstractWriter;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.util.AbstractTypeBag;
import org.eclipse.imp.pdb.facts.util.EqualityUtils;
import org.eclipse.imp.pdb.facts.util.TransientSet;
import org.eclipse.imp.pdb.facts.util.TrieSet;

/*package*/class TemporarySetWriter1 extends AbstractWriter implements
		ISetWriter {
	
	@SuppressWarnings({ "unchecked", "unused" })
	private static final Comparator<Object> equalityComparator = EqualityUtils.getDefaultEqualityComparator();
	
	@SuppressWarnings("unchecked")
	private static final Comparator<Object> equivalenceComparator = EqualityUtils.getEquivalenceComparator();

	protected final AbstractTypeBag elementTypeBag;
	protected final TransientSet<IValue> setContent;

	protected final boolean checkUpperBound;
	protected final Type upperBoundType;
	protected ISet constructedSet;

	/* package */TemporarySetWriter1(Type upperBoundType) {
		super();

		this.checkUpperBound = true;
		this.upperBoundType = upperBoundType;
		
		elementTypeBag = AbstractTypeBag.of();
		setContent = TrieSet.transientOf();
	}

	/* package */TemporarySetWriter1() {
		super();
		
		this.checkUpperBound = false;
		this.upperBoundType = null;

		elementTypeBag = AbstractTypeBag.of();
		setContent = TrieSet.transientOf();
	}

	private void put(IValue element) {
		final Type elementType = element.getType();

		if (checkUpperBound && !elementType.isSubtypeOf(upperBoundType)) {
			throw new UnexpectedElementTypeException(upperBoundType, elementType);
		}

		elementTypeBag.increase(elementType);
		setContent.__insertEquivalent(element, equivalenceComparator);
	}

	@Override
	public void insert(IValue... elems) throws FactTypeUseException {
		checkMutation();

		for (IValue elem : elems) {
			put(elem);
		}
	}

	@Override
	public void insertAll(Iterable<? extends IValue> collection)
			throws FactTypeUseException {
		checkMutation();

		for (IValue v : collection) {
			put(v);
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
		if (constructedSet != null)
			throw new UnsupportedOperationException(
					"Mutation of a finalized set is not supported.");
	}
	
	@Override
	public String toString() {
		return setContent.toString();
	}

}
