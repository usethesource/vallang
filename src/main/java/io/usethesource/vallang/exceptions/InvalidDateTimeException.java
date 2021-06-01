package io.usethesource.vallang.exceptions;

public class InvalidDateTimeException extends FactTypeUseException {

	private static final long serialVersionUID = 5634976179346912971L;

	public InvalidDateTimeException(String message) {
		super(message);
	}

	public InvalidDateTimeException(String message, Throwable cause) {
		super(message, cause);
	}

}
