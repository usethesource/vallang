package io.usethesource.vallang.exceptions;

public class TypeReificationException extends FactTypeUseException {
	private static final long serialVersionUID = -1606508959996710935L;

	public TypeReificationException(String reason, Throwable cause) {
		super(reason, cause);
	}
}
