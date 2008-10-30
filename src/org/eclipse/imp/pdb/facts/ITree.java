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

import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/**
 * Typed trees. Trees are typed according to an algebraic signature (grammar).
 * @see TypeFactory and @see TreeType and @see TreeSortType
 * 
 * @author jurgenv
 *
 */
public interface ITree extends IValue, Iterable<IValue> {
	public IValue get(int i);
	public IValue get(String label);
	public ITree  set(int i, IValue newChild);
	public ITree  set(String label, IValue newChild);
	public int arity();
	public String getName();
	public TreeNodeType getTreeNodeType();
	public TupleType getChildrenTypes();
	public Iterable<IValue> getChildren();
}
