package org.eclipse.imp.pdb.facts.impl.hash;

import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of a typed tree node with access to children via labels
 */
public class Node extends Tree implements INode {
	
	/*package*/ Node(TreeNodeType type, IValue[] children) {
		super(type.getName(), children);
		fType = type;
	}
	
	/*package*/ Node(TreeNodeType type) {
		this(type, new IValue[0]);
	}

	private Node(Node node, String label, IValue anno) {
		super(node, label, anno);
		fType = node.fType;
	}

	private Node(Node other, int childIndex, IValue newChild) {
		super(other, childIndex, newChild);
		fType = other.fType;
	}

	protected final TreeNodeType fType;

	public IValue get(String label) {
		return super.get(((TreeNodeType) fType).getChildIndex(label));
	}

	public TupleType getChildrenTypes() {
		return ((TreeNodeType) fType).getChildrenTypes();
	}

	public TreeNodeType getTreeNodeType() {
		return fType;
	}

	@Override
	public Type getType() {
		return fType.getSuperType();
	}

	@Override
	public INode set(int i, IValue newChild) throws IndexOutOfBoundsException {
		checkChildType(i, newChild);
		return new Node(this, i, newChild);
	}

	
	public INode set(String label, IValue newChild) throws FactTypeError {
		int childIndex = ((TreeNodeType) fType).getChildIndex(label);
		checkChildType(childIndex, newChild);
		return new Node(this, childIndex, newChild);
	}
	
	private void checkChildType(int i, IValue newChild) {
		Type type = newChild.getType();
		Type expectedType = getTreeNodeType().getChildType(i);
		if (!type.isSubtypeOf(expectedType)) {
			throw new FactTypeError("New child type " + type + " is not a subtype of " + expectedType);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Node) {
		  return fType == ((Node) obj).fType && super.equals(obj);
		}
		return false;
	}
	
	@Override
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitNode(this);
	}
	
	@Override
	protected IValue clone(String label, IValue anno) {
		return new Node(this, label, anno);
	}
}
