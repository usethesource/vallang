package org.eclipse.imp.pdb.facts.visitors;

public class VisitorException extends Exception {
	private static final long serialVersionUID = 4217829420715152023L;

	public VisitorException(String msg) {
		super(msg);
	}
	
	public VisitorException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	public VisitorException(Throwable cause) {
		super(cause);
	}
}
