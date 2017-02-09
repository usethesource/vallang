package io.usethesource.vallang.exceptions;

public class NullTypeException extends FactTypeUseException {
	private static final long serialVersionUID = -4201840676263159311L;

	public NullTypeException() {
		super("A null reference as a type");
	}

}
