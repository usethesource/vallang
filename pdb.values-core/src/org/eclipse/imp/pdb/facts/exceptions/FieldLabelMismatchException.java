package org.eclipse.imp.pdb.facts.exceptions;

public class FieldLabelMismatchException extends FactTypeDeclarationException {
	private static final long serialVersionUID = 3510697170437859049L;
	private int fields;
	private int labels;

	public FieldLabelMismatchException(int fields, int labels) {
		super("Expected " + fields + " field labels, but got " + labels);
		this.fields = fields;
		this.labels = labels;
	}
	
	public int getFields() {
		return fields;
	}
	
	public int getLabels() {
		return labels;
	}

}
