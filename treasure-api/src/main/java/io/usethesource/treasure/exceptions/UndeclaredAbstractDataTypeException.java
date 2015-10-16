package io.usethesource.treasure.exceptions;

import io.usethesource.treasure.type.Type;

public class UndeclaredAbstractDataTypeException extends
		FactTypeDeclarationException {
	private static final long serialVersionUID = 2192451595458909479L;

	public UndeclaredAbstractDataTypeException(Type adt) {
		super(adt + " is not registered");
	}
}
