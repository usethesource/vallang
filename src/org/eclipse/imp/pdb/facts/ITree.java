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


/**
 * Untyped tree representation, can iterate over the list of children.
 *
 */
public interface ITree extends IValue, Iterable<IValue> {
	/**
	 * Get a child
	 * @param i the zero based index of the child
	 * @return a value
	 */
	public IValue get(int i);
	
	/**
	 * Change this tree to have a different child at a certain position.
	 * 
	 * @param <TreeOrNode> may be a untyped tree or a typed tree node that is returned, depending on the receiver.
	 * @param i            the zero based index of the child to be replaced
	 * @param newChild     the new value for the child
	 * @return an untyped tree with the new child at position i, if the receiver was untyped,
	 *         or a typed tree node if it was typed.
	 */
	public <TreeOrNode extends ITree> TreeOrNode set(int i, IValue newChild);
	
	/**
	 * @return the (fixed) number of children of this tree
	 */
	public int arity();
	
	/**
	 * @return the name of this tree (an identifier)
	 */
	public String getName();
	
	/**
	 * @return an iterator over the children, equivalent to 'this'.
	 */
	public Iterable<IValue> getChildren();
}
