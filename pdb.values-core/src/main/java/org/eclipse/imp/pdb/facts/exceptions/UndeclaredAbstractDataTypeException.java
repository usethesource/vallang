package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class UndeclaredAbstractDataTypeException extends
		FactTypeDeclarationException {
	private static final long serialVersionUID = 2192451595458909479L;

	public UndeclaredAbstractDataTypeException(Type adt) {
		super(adt + " is not registered");
	}
}
