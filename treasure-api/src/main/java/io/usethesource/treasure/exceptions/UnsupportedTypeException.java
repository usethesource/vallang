package io.usethesource.treasure.exceptions;

import io.usethesource.treasure.type.Type;

public class UnsupportedTypeException extends FactTypeUseException {
	private static final long serialVersionUID = -8995093767494157052L;
	private Type type;
	public UnsupportedTypeException(String explanation, Type type) {
		super(explanation);
		this.type = type;
	}
	
	public Type getType() {
		return type;
	}
}
