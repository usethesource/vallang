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
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.func;

import java.util.ArrayList;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

public class NodeFunctions {
	
	/*
     * TODO: merge with ListFunctions.replace(...). Algorithm is exactly the same, the only difference is
     * that difference interfaces are used (IList, INode).
	 */
	public static INode replace(IValueFactory vf, INode node1, int first, int second, int end, IList repl)
			throws FactTypeUseException, IndexOutOfBoundsException {
		ArrayList<IValue> newChildren = new ArrayList<>();
		int rlen = repl.length();
		int increment = Math.abs(second - first);
		if (first < end) {
			int childIndex = 0;
			// Before begin
			while (childIndex < first) {
				newChildren.add(node1.get(childIndex++));
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
					newChildren.add(node1.get(childIndex++));
				}
			}
			if (!wrapped) {
				while (replIndex < rlen) {
					newChildren.add(repl.get(replIndex++));
				}
			}
			// After end
			int dlen = node1.arity();
			while (childIndex < dlen) {
				newChildren.add(node1.get(childIndex++));
			}
		} else {
			// Before begin (from right to left)
			int childIndex = node1.arity() - 1;
			while (childIndex > first) {
				newChildren.add(0, node1.get(childIndex--));
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
					newChildren.add(0, node1.get(childIndex--));
				}
			}
			if (!wrapped) {
				while (replIndex < rlen) {
					newChildren.add(0, repl.get(replIndex++));
				}
			}
			// Left of end
			while (childIndex >= 0) {
				newChildren.add(0, node1.get(childIndex--));
			}
		}

		IValue[] childArray = new IValue[newChildren.size()];
		newChildren.toArray(childArray);
		return vf.node(node1.getName(), childArray);
	}

	public static IValue getKeywordArgumentValue(IValueFactory vf, INode node1, String name) {
		if (node1.hasKeywordArguments()) {
			int k = node1.getKeywordIndex(name);
			if (k >= 0)
				return node1.get(k);
		}
		return null;
	}

	public static boolean hasKeywordArguments(IValueFactory vf, INode node1) {
		return node1.getKeywordArgumentNames() != null;
	}

	public static int getKeywordIndex(IValueFactory vf, INode node1, String name) {
		if (node1.hasKeywordArguments()) {
			String[] keyArgNames = node1.getKeywordArgumentNames();
			for (int i = 0; i < keyArgNames.length; i++) {
				if (name.equals(keyArgNames[i])) {
					return node1.positionalArity() + i;
				}
			}
		}
		return -1;
	}

	public static int positionalArity(IValueFactory vf, INode node1) {
		if (node1.hasKeywordArguments())
			return node1.arity() - node1.getKeywordArgumentNames().length;
		else
			return node1.arity();
	}
	
	public static boolean isEqual(IValueFactory vf, INode node1, IValue value) {
		if(value == node1) return true;
		if(value == null) return false;
		
		if (value instanceof INode) {
			INode node2 = (INode) value;
			
			// Object equality ('==') is not applicable here
			// because value is cast to {@link INode}.
			if (!node1.getName().equals(node2.getName())) {
				return false;
			}

//			if (node1.arity() == other.arity()) {
//				if (node1.positionalArity() != other.positionalArity()) {
//					return false;
//				}
//				
//				final Iterator<IValue> it1 = node1.iterator();
//				final Iterator<IValue> it2 = other.iterator();
//				
//				while (it1.hasNext() && it2.hasNext()) {
//					// call to IValue.isEqual(IValue)
//					if (it1.next().isEqual(it2.next()) == false)
//						return false;
//				}
//
//				assert (!it1.hasNext() && !it2.hasNext());
//				return true;
//				...

			
			int nrOfChildren = node1.arity();
			if (nrOfChildren == node2.arity()) {
				int nrOfPosChildren = node1.positionalArity();
				if (nrOfPosChildren != node2.positionalArity()) {
					return false;
				}
				for (int i = nrOfPosChildren - 1; i >= 0; i--) {
					if (!node2.get(i).isEqual(node1.get(i)))
						return false;
				}

				if (nrOfPosChildren < nrOfChildren) {
					if (!node1.hasKeywordArguments())
						return false;

					final String[] keyArgNames = node1
							.getKeywordArgumentNames();

					for (int i = 0; i < keyArgNames.length; i++) {
						String kw = keyArgNames[i];
						int k = node2.getKeywordIndex(kw);
						if (k < 0 || !node1.get(nrOfPosChildren + i).isEqual(node2.get(k))) {
							return false;
						}
					}
				}
				return true;
			}
		}
		return false;
	}

}
