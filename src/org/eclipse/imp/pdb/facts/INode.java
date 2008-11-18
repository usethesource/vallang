package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TupleType;

/**
 * Typed tree representation. An INode is a specific kind of ITree, namely one
 * that adheres to a specific schema see @link{TreeNodeType}.
 *
 */
public interface INode extends ITree {
	public IValue get(String label);
	public ITree  set(String label, IValue newChild);
	public TreeNodeType getTreeNodeType();
	public TupleType getChildrenTypes();
}
