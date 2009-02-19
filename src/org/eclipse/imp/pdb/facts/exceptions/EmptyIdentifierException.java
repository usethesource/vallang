package org.eclipse.imp.pdb.facts.exceptions;

public class EmptyIdentifierException extends FactTypeDeclarationException {
	private static final long serialVersionUID = -7032740671919237233L;

	public EmptyIdentifierException() {
		super("An identifier can not be empty or null");
	}
}
