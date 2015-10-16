/*******************************************************************************
 * Copyright (c) 2009 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI  
 *******************************************************************************/
package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeStore;

/**
 * Typed node representation. An IConstructor is a specific kind of INode, namely one
 * that adheres to a specific schema see @link{ConstructorType}.
 *
 */
public interface IConstructor extends INode {

	/**
	 * @return the AbstractDataType of a constructor.
	 */
	public Type getType();
	
	/**
	 * @return the specific ConstructorType of this constructor
	 */
	public Type getConstructorType();
	
	/**
   * @return the specific ConstructorType of this constructor but before instantiating
   * type parameters. This is needed for serialization purposes.
   */
	public Type getUninstantiatedConstructorType();
	
	/**
	 * Get a child from a labeled position in the tree.
	 * @param label the name of the child
	 * @return a value at the position indicated by the label.
	 */
	public IValue  get(String label);
	
	/**
	 * Replace a child at a labeled position in the tree. 
	 * @param label    the label of the position
	 * @param newChild the new value of the child
	 * @return a new tree node that is the same as the receiver except for
	 * the fact that at the labeled position the new value has replaced the old value.
	 * All annotations remain equal.
	 * 
	 * @throws FactTypeUseException when this label does not exist for the given tree node, or 
	 *         when the given value has a type that is not a sub-type of the declared type
	 *         of the child with this label.
	 */
	public IConstructor set(String label, IValue newChild)
			throws FactTypeUseException;
	
	/**
	 * Find out whether this constructor has a field a given name
	 * @param label name of the field
	 * 
	 * @return true iff this constructor has this field name
	 */
	public boolean has(String label);
	
	/**
	 * Replace a child at an indexed position in the tree. 
	 * @param label    the label of the position
	 * @param newChild the new value of the child
	 * @return a new tree node that is the same as the receiver except for
	 * the fact that at the labeled position the new value has replaced the old value.
	 * All annotations remain equal.
	 * 
	 * @throws FactTypeUseException when the index is greater than the arity of this tree node, or 
	 *         when the given value has a type that is not a sub-type of the declared type
	 *         of the child at this index.
	 */
	public IConstructor set(int index, IValue newChild)
			throws FactTypeUseException;
	
	/**
	 * @return a tuple type representing the children types of this node/
	 */
	public Type getChildrenTypes();
	
	/**
	 * Check whether a certain annotation label is declared for this type of node.
	 * @param label
	 * @return true iff the given annotation label was declared for this type of node.
	 */
	public boolean declaresAnnotation(TypeStore store, String label);
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.imp.pdb.facts.IValue#asAnnotatable()
	 */
	public IAnnotatable<? extends IConstructor> asAnnotatable();
	
	/*
   * (non-Javadoc)
   * @see org.eclipse.imp.pdb.facts.IValue#asWithKeywordParameters()
   */
  public IWithKeywordParameters<? extends IConstructor> asWithKeywordParameters();
}
