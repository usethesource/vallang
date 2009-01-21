/*******************************************************************************
* Copyright (c) CWI 2008 
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju (jurgenv@cwi.nl) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts;

import java.util.Iterator;


/**
 * Untyped node representation (a container that has a limited amount of children and a name), 
 * can iterate over the list of children. This data construct can be used to make trees by applying
 * it recursively.
 */
public interface INode extends IValue, Iterable<IValue> {
	/**
	 * Get a child
	 * @param i the zero based index of the child
	 * @return a value
	 */
	public IValue get(int i) throws IndexOutOfBoundsException;
	
	/**
	 * Change this tree to have a different child at a certain position.
	 * 
	 * @param i            the zero based index of the child to be replaced
	 * @param newChild     the new value for the child
	 * @return an untyped tree with the new child at position i, if the receiver was untyped,
	 *         or a typed tree node if it was typed.
	 */
	public INode set(int i, IValue newChild) throws IndexOutOfBoundsException;
	
	/**
	 * @return the (fixed) number of children of this node
	 */
	public int arity();
	
	/**
	 * @return the name of this node (an identifier)
	 */
	public String getName();
	
	/**
	 * @return an iterator over the direct children, equivalent to 'this'.
	 */
	public Iterable<IValue> getChildren();
	
	/**
	 * @return an iterator over the direct children.
	 */
	public Iterator<IValue> iterator();
}
