package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class FactMatchException extends FactTypeUseException {
	private static final long serialVersionUID = 3043093533006947077L;

	private Type subject;
	private Type pattern;
	
	public FactMatchException(Type pattern, Type subject) {
		super(subject + " does not match " + pattern);
	}
	
	public Type getSubject() {
		return subject;
	}
	
	public Type getPattern() {
		return pattern;
	}
}
