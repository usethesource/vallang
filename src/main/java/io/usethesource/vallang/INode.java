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

package io.usethesource.vallang;

import java.util.ArrayList;
import java.util.Iterator;

import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.visitors.IValueVisitor;


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
	 * @throws IndexOutOfBoundsException
	 */
	public IValue get(int i);
	
	/**
	 * Change this tree to have a different child at a certain position.
	 * 
	 * @param i            the zero based index of the child to be replaced
	 * @param newChild     the new value for the child
	 * @return an untyped tree with the new child at position i, if the receiver was untyped,
	 *         or a typed tree node if it was typed.
	 * @throws IndexOutOfBoundsException
	 */
	public INode set(int i, IValue newChild);
	
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
	public default INode replace(int first, int second, int end, IList repl) {
	    ArrayList<IValue> newChildren = new ArrayList<>();
	    int rlen = repl.length();
	    int increment = Math.abs(second - first);
	    if (first < end) {
	        int childIndex = 0;
	        // Before begin
	        while (childIndex < first) {
	            newChildren.add(this.get(childIndex++));
	        }
	        int replIndex = 0;
	        boolean wrapped = false;
	        // Between begin and end
	        while (childIndex < end) {
	            newChildren.add(repl.get(replIndex++));
	            if (replIndex == rlen) {
	                replIndex = 0;
	                wrapped = true;
	            }
	            childIndex++; //skip the replaced element
	            for (int j = 1; j < increment && childIndex < end; j++) {
	                newChildren.add(this.get(childIndex++));
	            }
	        }
	        if (!wrapped) {
	            while (replIndex < rlen) {
	                newChildren.add(repl.get(replIndex++));
	            }
	        }
	        // After end
	        int dlen = this.arity();
	        while (childIndex < dlen) {
	            newChildren.add(this.get(childIndex++));
	        }
	    } else {
	        // Before begin (from right to left)
	        int childIndex = this.arity() - 1;
	        while (childIndex > first) {
	            newChildren.add(0, this.get(childIndex--));
	        }
	        // Between begin (right) and end (left)
	        int replIndex = 0;
	        boolean wrapped = false;
	        while (childIndex > end) {
	            newChildren.add(0, repl.get(replIndex++));
	            if (replIndex == repl.length()) {
	                replIndex = 0;
	                wrapped = true;
	            }
	            childIndex--; //skip the replaced element
	            for (int j = 1; j < increment && childIndex > end; j++) {
	                newChildren.add(0, this.get(childIndex--));
	            }
	        }
	        if (!wrapped) {
	            while (replIndex < rlen) {
	                newChildren.add(0, repl.get(replIndex++));
	            }
	        }
	        // Left of end
	        while (childIndex >= 0) {
	            newChildren.add(0, this.get(childIndex--));
	        }
	    }

	    IValue[] childArray = new IValue[newChildren.size()];
	    newChildren.toArray(childArray);
	    
	    return setChildren(childArray);
	}
    
	/**
	 * Replace all children with a new array of children, keeping only
	 * the name of the original node.
	 * @param childArray
	 * @return a new node with the same name, yet new children.
	 */
	public INode setChildren(IValue[] childArray);

    @Override
    public default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
	    return v.visitNode(this);
	}
	
    /*
     * (non-Javadoc)
     * @see IValue#asAnnotatable()
     */
    public IAnnotatable<? extends INode> asAnnotatable();
    
    /*
     * (non-Javadoc)
     * @see IValue#asWithKeywordParameters()
     */
    public IWithKeywordParameters<? extends INode> asWithKeywordParameters();
    
    @Override
    public default boolean isEqual(IValue value) {
        if(value == this) return true;
        if(value == null) return false;

        if (this.getType() != value.getType()) {
            return false;
        }

        if (value instanceof INode) {
            INode node2 = (INode) value;

            // Object equality ('==') is not applicable here
            // because value is cast to {@link INode}.
            if (!this.getName().equals(node2.getName())) {
                return false;
            }

            if (this.arity() != node2.arity()) {
                return false;
            }

            Iterator<IValue> it1 = this.iterator();
            Iterator<IValue> it2 = node2.iterator();

            while (it1.hasNext()) {
                IValue o1 = it1.next();
                IValue o2 = it2.next();

                if (!o1.isEqual(o2)) {
                    return false;
                }
            }

            if (this.mayHaveKeywordParameters() && node2.mayHaveKeywordParameters()) {
                return this.asWithKeywordParameters().equalParameters(node2.asWithKeywordParameters());
            }

            if (this.mayHaveKeywordParameters() && this.asWithKeywordParameters().hasParameters()) {
                return false;
            }

            if (node2.mayHaveKeywordParameters() && node2.asWithKeywordParameters().hasParameters()) {
                return false;
            }

            return true;
        }

        return false;
    }
    
    @Override
    public default boolean match(IValue value) {
        if(value == this) return true;
        if(value == null) return false;

        if (this == value) {
            return true;
        }
        
        if (getType() != value.getType()) {
            return false;
        }

        if (value instanceof INode) {
            INode node2 = (INode) value;

            // Object equality ('==') is not applicable here
            // because value is cast to {@link INode}.
            if (!getName().equals(node2.getName())) {
                return false;
            }

            if (arity() != node2.arity()) {
                return false;
            }

            Iterator<IValue> it1 = iterator();
            Iterator<IValue> it2 = node2.iterator();

            while (it1.hasNext()) {
                if (!it1.next().match(it2.next())) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }
}
