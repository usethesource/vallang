package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class UnexpectedTypeException extends FactTypeUseException {
	private static final long serialVersionUID = -5107803679675463540L;
	private Type expected;
	private Type got;

	public UnexpectedTypeException(Type expected, Type got) {
		super("Expected " + expected + ", but got " + got);
		this.expected = expected;
		this.got = got;
	}

	public boolean hasExpected() {
		return expected != null;
	}
	
	public Type getExpected() {
		return expected;
	}

	public Type getGiven() {
		return got;
	}


}
