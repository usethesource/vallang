package org.eclipse.imp.pdb.facts.impl.hash;

import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of a typed tree node with access to children via labels
 */
public class Node extends Tree implements INode {
	
	/*package*/ Node(Type type, IValue[] children) {
		super(type.getName(), type, children);
	}
	
	/*package*/ Node(Type type) {
		this(type, new IValue[0]);
	}
	
	@Override
	public Type getType() {
		// TODO Auto-generated method stub
		return super.getType();
	}

	private Node(Node node, String label, IValue anno) {
		super(node, label, anno);
	}

	private Node(Node other, int childIndex, IValue newChild) {
		super(other, childIndex, newChild);
	}

	public IValue get(String label) {
		return super.get(fType.getFieldIndex(label));
	}

	public Type getChildrenTypes() {
		return fType.getFieldTypes();
	}

	@Override
	public INode set(int i, IValue newChild) throws IndexOutOfBoundsException {
		checkChildType(i, newChild);
		return new Node(this, i, newChild);
	}

	
	public INode set(String label, IValue newChild) throws FactTypeError {
		int childIndex = fType.getFieldIndex(label);
		checkChildType(childIndex, newChild);
		return new Node(this, childIndex, newChild);
	}
	
	private void checkChildType(int i, IValue newChild) {
		Type type = newChild.getType();
		Type expectedType = getType().getFieldType(i);
		if (!type.isSubtypeOf(expectedType)) {
			throw new FactTypeError("New child type " + type + " is not a subtype of " + expectedType);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (getClass() == obj.getClass()) {
		  return fType.comparable(((Node) obj).fType) && super.equals(obj);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		 return fType.hashCode() + ~super.hashCode();
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
