/*******************************************************************************
* Copyright (c) 2011 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Anya Helene Bagge - initial API and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.test.random;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *	A unified generic data generator for random data and supplied static data values.
 *
 *  Register your data genererators for particular types by calling the constructor
 *  or the addGenerator method.
 *  
 *  Call generator(DataType.class, n) to obtain a generator for a particular
 *  data type.
 */
public class DataGenerator {

	protected final Map<Class<?>, List<? extends Object>> staticValues = new HashMap<>();
	protected final Map<Class<?>, RandomGenerator<?>> random = new HashMap<>();

	public DataGenerator() {
	}

	/**
	 *	Make a new DataGenerator, building on <code>base</code>, adding
	 *  a generator for a particular type.
	 */
	public <T> DataGenerator(DataGenerator base, Class<T> type, List<? extends T> staticValues, RandomGenerator<? extends T> random) {
		this.staticValues.putAll(base.staticValues);
		this.staticValues.put(type, staticValues);
		this.random.putAll(base.random);
		this.random.put(type,  random);
	}

	/**
	 *  Make a new DataGenerator with a single generator for a particular type.
	 */
	public <T> DataGenerator(Class<T> type, List<T> staticValues, RandomGenerator<T> random) {
		this.staticValues.put(type, staticValues);
		this.random.put(type,  random);
	}

	/**
	 *  Add another single-type generator to this DataGenerator.
	 */
	public <T> void addGenerator(Class<T> type, List<T> staticValues, RandomGenerator<T> random) {
		this.staticValues.put(type, staticValues);
		this.random.put(type,  random);
	}

	/**
	 * Will supply all the static values provided for the type, followed
	 * by n random values of the type.
	 * 
	 * @return An iterator over values of the given type.
	 */
	public <T> Iterable<T> generate(Class<T> type, int n) {
		if(staticValues.containsKey(type)) {
			return new DataIterable<>((List<T>)staticValues.get(type), (RandomGenerator<T>)random.get(type), n);
		}
		else {
			throw new IllegalArgumentException("Don't know how do create data of type " + type.getName());
		}
	}
}

class DataIterable<T> implements Iterable<T> {
	protected final List<T> staticValues;
	protected final RandomGenerator<T> random;
	protected final int n;

	public DataIterable(List<T> staticValues, RandomGenerator<T> random, int n) {
		this.staticValues = staticValues;
		this.random = random;
		this.n = n;
	}

	public Iterator<T> iterator() {
		return new DataIterator(staticValues.iterator(), n);
	}


	class DataIterator implements Iterator<T> {

		private Iterator<T> values;
		private int n;

		public DataIterator(Iterator<T> values, int n) {
			this.values = values;
			this.n = n;
		}

		public boolean hasNext() {
			return values.hasNext() || n > 0;
		}

		public T next() {
			if(values.hasNext())
				return values.next();
			else if(n-- > 0)
				return random.next();
			return null;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

	}
}
