/*******************************************************************************
* Copyright (c) CWI 2008 
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju (jurgenv@cwi.nl) - initial API and implementation
*    Paul Klint (Paul.Klint@cwi.nl) - added replace
*    Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
*******************************************************************************/

package org.eclipse.imp.pdb.facts;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;


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
	 * @return the (fixed) number of children of this node (excluding keyword arguments)
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
	
	 /**
     * Replaces the value of the children first, second ... end with the elements in the list r
     * Expected:
     * - support for negative indices
     * - support for the case begin > end
     * @param first index to start replacement (inclusive)
     * @param second index of second element
     * @param end index to end replacement (exclusive)
     * @param repl the new values to replace the old one
     * @return a new node with the children replaced
     * @throws FactTypeUseException when the type of the element is not a subtype of the element type
     * @throws IndexOutOfBoundsException when the b < 0 or b >= INode.arity() or e < 0 or e > INOde.arity()
     */
    public INode replace(int first, int second, int end, IList repl) throws FactTypeUseException, IndexOutOfBoundsException;
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.imp.pdb.facts.IValue#asAnnotatable()
     */
    public IAnnotatable<? extends INode> asAnnotatable();
    
    /*
     * (non-Javadoc)
     * @see org.eclipse.imp.pdb.facts.IValue#asWithKeywordParameters()
     */
    public IWithKeywordParameters<? extends INode> asWithKeywordParameters();
}
