package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class IllegalInstantiatedAbstractDataTypeException extends
		FactTypeDeclarationException {
	private static final long serialVersionUID = -7171289358305194254L;

	public IllegalInstantiatedAbstractDataTypeException(Type adt) {
		super("should not declare instances of type-parametrized abstract data-types (" + adt + ")");
	}
}
