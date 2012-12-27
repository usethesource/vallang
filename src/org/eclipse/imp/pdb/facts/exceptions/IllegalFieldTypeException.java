package org.eclipse.imp.pdb.facts.exceptions;

public class IllegalFieldTypeException extends FactTypeDeclarationException {
	private static final long serialVersionUID = -8845629423612702596L;
	private int pos;
	private Object elem;

	public IllegalFieldTypeException(int pos, Object elem, ClassCastException cause) {
		super("Expected a field type at position " + pos + ", but got something different", cause);
		this.pos = pos;
		this.elem = elem;
	}

	public Object getElement() {
		return elem;
	}
	
	public int getPos() {
		return pos;
	}
	
	public synchronized ClassCastException getCause() {
		return (ClassCastException) super.getCause();
	}
}
