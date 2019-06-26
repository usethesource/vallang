/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import java.util.Iterator;
import java.util.NoSuchElementException;

import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.visitors.IValueVisitor;

/*package*/ class Tuple implements ITuple{
	
	protected static final TypeFactory typeFactory = TypeFactory.getInstance();

	private Type cachedTupleType;
	protected final IValue[] elements;

	public static ITuple newTuple(IValue... elements) {
		return new Tuple(elements);
	}

	private Tuple(IValue... elements) {
		super();
		
		this.cachedTupleType = null;
		this.elements = elements;
	}

	@Override
	public Type getType() {
		if (cachedTupleType == null) {
			cachedTupleType = TypeFactory.getInstance().tupleType(elements);
		}
		
		return cachedTupleType;
	}

	@Override
	public int arity() {
		return elements.length;
	}

	@Override
	public IValue get(int i) {
		return elements[i];
	}

	@Override
	public IValue get(String label) {
		return elements[getType().getFieldIndex(label)];
	}

	@Override
	public Iterator<IValue> iterator() {
		return new TupleIterator(this);
	}

	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
		return v.visitTuple(this);
	}

	@Override
	public ITuple set(int index, IValue arg) {
		int nrOfElements = elements.length;
		IValue[] newElements = new IValue[nrOfElements];
		Type[] elementTypes = new Type[nrOfElements];
		for (int i = nrOfElements - 1; i >= 0; i--) {
			IValue element = elements[i];
			newElements[i] = element;
			elementTypes[i] = element.getType();
		}

		newElements[index] = arg;
		elementTypes[index] = arg.getType();

		return new Tuple(newElements);
	}

	@Override
	public ITuple set(String label, IValue arg) {
		int nrOfElements = elements.length;
		IValue[] newElements = new IValue[nrOfElements];
		Type[] elementTypes = new Type[nrOfElements];
		for (int i = nrOfElements - 1; i >= 0; i--) {
			IValue element = elements[i];
			newElements[i] = element;
			elementTypes[i] = element.getType();
		}

		newElements[getType().getFieldIndex(label)] = arg;
		elementTypes[getType().getFieldIndex(label)] = arg.getType();

		return new Tuple(newElements);
	}

	@Override
	public IValue select(int... indexes) {
		if (indexes.length == 1)
			return get(indexes[0]);

		int nrOfElements = indexes.length;
		IValue[] elements = new IValue[nrOfElements];
		Type[] elementTypes = new Type[nrOfElements];
		for (int i = nrOfElements - 1; i >= 0; i--) {
			IValue element = get(indexes[i]);
			elements[i] = element;
			elementTypes[i] = element.getType();
		}

		return new Tuple(elements);
	}

	@Override
	public IValue selectByFieldNames(String... fields) {
		if (fields.length == 1)
			return get(fields[0]);

		int nrOfElements = fields.length;
		IValue[] elements = new IValue[nrOfElements];
		Type[] elementTypes = new Type[nrOfElements];
		for (int i = nrOfElements - 1; i >= 0; i--) {
			IValue element = get(fields[i]);
			elements[i] = element;
			elementTypes[i] = element.getType();
		}

		return new Tuple(elements);
	}

	@Override
	public String toString() {
	    return defaultToString();
	}
	
	@Override
	public int hashCode() {
		int hash = 1331;

		for (int i = elements.length - 1; i >= 0; i--) {
			hash -= (hash << 19) + (hash >>> 8);
			hash ^= elements[i].hashCode();
		}

		return hash - (hash << 7);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;

		if (o.getClass() == getClass()) {
			Tuple otherTuple = (Tuple) o;

			IValue[] otherElements = otherTuple.elements;
			int nrOfElements = elements.length;
			if (otherElements.length == nrOfElements) {
				for (int i = nrOfElements - 1; i >= 0; i--) {
					if (!otherElements[i].equals(elements[i]))
						return false;
				}
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isEqual(IValue value) {
		if (value == this)
			return true;
		if (value == null)
			return false;

		if (value instanceof Tuple) {
			Tuple otherTuple = (Tuple) value;

			if (!getType().comparable(otherTuple.getType()))
				return false;

			IValue[] otherElements = otherTuple.elements;
			int nrOfElements = elements.length;
			if (otherElements.length == nrOfElements) {
				for (int i = nrOfElements - 1; i >= 0; i--) {
					if (!otherElements[i].isEqual(elements[i]))
						return false;
				}
				return true;
			}
		}

		return false;
	}

	
	@Override
    public boolean match(IValue value) {
        if (value == this)
            return true;
        if (value == null)
            return false;

        if (value instanceof Tuple) {
            Tuple otherTuple = (Tuple) value;

            if (!getType().comparable(otherTuple.getType()))
                return false;

            IValue[] otherElements = otherTuple.elements;
            int nrOfElements = elements.length;
            if (otherElements.length == nrOfElements) {
                for (int i = nrOfElements - 1; i >= 0; i--) {
                    if (!otherElements[i].match(elements[i]))
                        return false;
                }
                return true;
            }
        }

        return false;
    }
	
	private static class TupleIterator implements Iterator<IValue> {
		private final IValue[] elements;
		private int index = 0;

		public TupleIterator(Tuple tuple) {
			super();

			elements = tuple.elements;
		}

		@Override
		public boolean hasNext() {
			return index < elements.length;
		}

		@Override
		public IValue next() {
			if (!hasNext())
				throw new NoSuchElementException("No more elements in this iteration.");

			return elements[index++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("This iterator doesn't support removal.");
		}
	}
}
