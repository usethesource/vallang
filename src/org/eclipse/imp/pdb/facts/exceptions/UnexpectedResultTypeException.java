package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;


public class UnexpectedResultTypeException extends FactTypeUseException {
	private static final long serialVersionUID = 1551922923060851569L;
	private Type result;

	public UnexpectedResultTypeException(Type result, Throwable cause) {
		super("Unexpected result " + result, cause);
		this.result = result;
	}

	public Type getResult() {
		return result;
	}
}
