package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class RedeclaredConstructorException extends
		FactTypeDeclarationException {
	private static final long serialVersionUID = 8330548728032865311L;
	private String name;
	private Type firstArgs;
	private Type secondArgs;

	public RedeclaredConstructorException(String name, Type firstArgs, Type secondArgs) {
		super("Constructor " + name + " overloaded with comparable argument types: " + firstArgs + " and " + secondArgs);
		this.name = name;
		this.firstArgs = firstArgs;
		this.secondArgs = secondArgs;
	}

	public String getName() {
		return name;
	}

	public Type getFirstArgs() {
		return firstArgs;
	}

	public Type getSecondArgs() {
		return secondArgs;
	}
}
