/*******************************************************************************
 * Copyright (c) 2014 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.util;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/**
 * Stores mapping (Type -> Integer) to keep track of a collection's element
 * types. The least upper bound type of is calculated on basis of the map keys.
 */
public abstract class AbstractTypeBag implements Cloneable {
		
	public abstract void increase(Type t);

	public abstract void decrease(Type t);
	
	public abstract Type lub();
	
	public abstract String getLabel();

	public abstract AbstractTypeBag clone();
	
	public static AbstractTypeBag of(Type... ts) {
		return of(null, ts);
	}
	
	public static AbstractTypeBag of(String label, Type... ts) {
		return new TypeBag(label, ts);
	}

	/**
	 * Implementation of <@link AbstractTypeBag/> that cached the current least
	 * upper bound.
	 */
	private static class TypeBag extends AbstractTypeBag {
		private final String label;
		private final Map<Type, Integer> countMap; // TODO: improve memory performance of internal map. 
		
		private Type cachedLub = null;

		private TypeBag(String label, Map<Type, Integer> countMap) {
			this.label = label;
			this.countMap = new HashMap<>(countMap);
		}
		
		private TypeBag(Type... ts) {
			this(null, ts);
		}
		
		private TypeBag(String label, Type... ts) {
			this.label = label;
			this.countMap = new HashMap<Type, Integer>(4);
			
			for (Type t : ts) {
				this.increase(t);
			}
		}
		
		@Override
		public void increase(Type t) {
			final Integer oldCount = countMap.get(t);
			if (oldCount == null) {
				countMap.put(t, 1);
				cachedLub = (cachedLub == null) ? null : cachedLub.lub(t); // update cached type
			} else {
				countMap.put(t, oldCount + 1);
			}
		}

		@Override
		public void decrease(Type t) {		
			final Integer oldCount = countMap.remove(t);
			if (oldCount == null) {
				throw new IllegalStateException(String.format("Type '%s' was not present.", t));
			} else if (oldCount > 1) {
				countMap.put(t, oldCount - 1);
			} else {
				// count was zero, thus do not reinsert
				cachedLub = null; // invalidate cached type
			}
		}
		
		@Override
		public Type lub() {
			if (cachedLub == null) {			
				Type inferredLubType = TypeFactory.getInstance().voidType();
				for (Type t : countMap.keySet()) {
					inferredLubType = inferredLubType.lub(t);
				}				
				cachedLub = inferredLubType;
			}
			return cachedLub;
		}	

		@Override
		public String getLabel() {
			return label;
		}

		@Override
		public AbstractTypeBag clone() {
			return new TypeBag(label, countMap);
		}
		
		@Override
		public String toString() {
			return countMap.toString();
		}
	}
	
}
