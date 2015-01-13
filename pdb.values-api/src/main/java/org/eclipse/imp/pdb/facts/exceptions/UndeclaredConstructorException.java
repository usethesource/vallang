package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class UndeclaredConstructorException extends FactTypeUseException {
	private static final long serialVersionUID = -6301806947030330971L;
	private Type adt;
	private String unknownName;
	private Type arguments;

	public UndeclaredConstructorException(Type adt, Type argTypes) {
		super(adt + " does not have a constructor to wrap these argument types: " + argTypes);
		this.adt = adt;
		this.arguments = argTypes;
	}

	public UndeclaredConstructorException(String constructorName) {
		super("A constructor with name " + constructorName + " was never declared");
		this.unknownName = constructorName;
	}
	
	public UndeclaredConstructorException(String constructorName, Type arguments) {
		super("A constructor with name " + constructorName + " and argument types " + arguments + " was never declared");
		this.unknownName = constructorName;
		this.arguments = arguments;
	}
	
	public String getName() {
		return unknownName;
	}

	public boolean hasArgumentTypes() {
		return arguments != null;
	}
	
	public Type getArgumentTypes() {
		return arguments;
	}
	
	public Type getADT() {
		return adt;
	}
}
