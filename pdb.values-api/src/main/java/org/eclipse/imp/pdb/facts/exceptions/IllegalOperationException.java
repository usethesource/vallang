package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class IllegalOperationException extends FactTypeUseException {
	private static final long serialVersionUID = 546504861606007094L;

	private Type lhs;
	private Type rhs;
	private String op;

	public IllegalOperationException(String op, Type lhs) {
		super("Operation " + op + " not allowed on " + lhs);
		this.lhs = lhs;
	}
		
	public IllegalOperationException(String op, Type lhs, Type rhs) {
		super("Operation " +op + " not allowed on " + lhs + " and " + rhs);
		this.op = op;
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public Type getLhs() {
		return lhs;
	}
	
	public Type getRhs() {
		return rhs;
	}
	
	public String getOperation() {
		return op;
	}

}
