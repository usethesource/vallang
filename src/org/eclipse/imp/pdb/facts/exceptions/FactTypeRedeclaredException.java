package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class FactTypeRedeclaredException extends FactTypeDeclarationException {
	private static final long serialVersionUID = 9191150588452685289L;
	private String name;
	private Type earlier;
	
	public FactTypeRedeclaredException(String name, Type earlier) {
		super(name + " was declared earlier as " + earlier);
		this.name = name;
		this.earlier = earlier;
	}

	public String getName() {
		return name;
	}
	
	public Type declaredEarlier() {
		return earlier;
	}
	
	
}
