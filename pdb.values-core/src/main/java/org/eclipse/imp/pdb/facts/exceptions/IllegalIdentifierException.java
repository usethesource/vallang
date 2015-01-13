package org.eclipse.imp.pdb.facts.exceptions;

public class IllegalIdentifierException extends FactTypeDeclarationException {
	private static final long serialVersionUID = 7380196205269771711L;
	private String id;

	public IllegalIdentifierException(String id) {
		super(id + " is not a valid identifier");
		this.id = id;
	}
	
	public String getIdentifier() {
		return id;
	}
}
