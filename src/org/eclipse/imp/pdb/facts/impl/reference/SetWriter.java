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
package org.eclipse.imp.pdb.facts.impl.reference;

import org.eclipse.imp.pdb.facts.IContainer;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedElementTypeException;
import org.eclipse.imp.pdb.facts.impl.Writer;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

import java.util.HashSet;
import java.util.Iterator;

/*package*/ class SetWriter extends Writer implements ISetWriter {
    protected final HashSet<IValue> setContent;
    protected final boolean inferred;
    protected Type eltType;
    protected Set constructedSet;

    /*package*/ SetWriter(Type eltType) {
        super();

        this.eltType = eltType;
        this.inferred = false;
        setContent = new HashSet<IValue>();
    }

    /*package*/ SetWriter() {
        super();
        this.eltType = TypeFactory.getInstance().voidType();
        this.inferred = true;
        setContent = new HashSet<IValue>();
    }

    private static void checkInsert(IValue elem, Type eltType) throws FactTypeUseException {
        Type type = elem.getType();
        if (!type.isSubtypeOf(eltType)) {
            throw new UnexpectedElementTypeException(eltType, type);
        }
    }

    private void put(IValue elem) {
        updateType(elem);
        checkInsert(elem, eltType);
        setContent.add(elem);
    }

    private void updateType(IValue elem) {
        if (inferred) {
            eltType = eltType.lub(elem.getType());
        }
    }

    @Override
	public void insert(IValue... elems) throws FactTypeUseException {
        checkMutation();

        for (IValue elem : elems) {
            put(elem);
        }
    }

    @Override
	public void insertAll(Iterable<? extends IValue> collection) throws FactTypeUseException {
        checkMutation();

        for (IValue v : collection) {
            put(v);
        }
    }

    @Override
	public ISet done() {
    	// Temporary fix of the static vs dynamic type issue
    	eltType = TypeFactory.getInstance().voidType();
    	for(IValue el : setContent)
    		eltType = eltType.lub(el.getType());
    	// ---
        if (constructedSet == null) {
            constructedSet = SetOrRel.apply(eltType, new SetContainer(setContent));
        }

        return constructedSet;
    }

    private void checkMutation() {
        if (constructedSet != null)
            throw new UnsupportedOperationException("Mutation of a finalized set is not supported.");
    }

    @Override
	public int size() {
        return constructedSet.size();
    }

    /*package*/ final class SetContainer implements IContainer {

    	/*package*/ SetContainer(java.util.HashSet<IValue> content) {
    		this.content = content;
    	}
    	
    	private final java.util.HashSet<IValue> content;
    	
		@Override
		public boolean isEmpty() {
			return content.isEmpty();
		}

		@Override
		public boolean hasCount() {
			return true;
		}

		@Override
		public int getCount() {
			return content.size();
		}

		@Override
		public boolean contains(IValue query) {
			return content.contains(query);
		}

		@Override
		public IValue get(IValue query) {
			if (contains(query)) {
				return query;
			} else {
				throw new IllegalArgumentException(); // how about none?
			}
		}

		@Override
		public int getArity() {
			return 1;
		}

		@Override
		public Iterator<IValue> iterator() {
			return content.iterator();
		}

		@Override
		public Iterator<IValue> iterator(int arity) {
			if (arity < 0 || arity >= getArity()) {
				throw new IllegalArgumentException(); // how about iterator over empty collection?
			}
			return content.iterator();
		}

		@Override
		public int hashCode() {			
			return content.hashCode();
//			int hash = 0;
//
//			Iterator<IValue> iterator = iterator();
//			while (iterator.hasNext()) {
//				IValue element = iterator.next();
//				hash = (hash << 1) ^ element.hashCode();
//			}
//
//			return hash;
		}

		@Override
		public boolean equals(Object other) {
	        if (other == this) return true;
	        if (other == null) return false;

	        if (other instanceof SetContainer) {
	            SetContainer that = (SetContainer) other;

	            return this.content.equals(that.content);
	        }
	        return false;		
        }
    	
    }    
    
}
