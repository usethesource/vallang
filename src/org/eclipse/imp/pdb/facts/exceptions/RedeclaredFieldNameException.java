package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class RedeclaredFieldNameException extends
		FactTypeDeclarationException {
	private static final long serialVersionUID = -4401062690006714904L;
	private String label;
	private Type one;
	private Type two;

	public RedeclaredFieldNameException(String label, Type one, Type two) {
		super("The field name " + label + " is illegaly used for both " + one + " and " + two);
		this.label = label;
		this.one = one;
		this.two = two;
	}
	
	public String getFieldName() {
		return label;
	}
	
	public Type getFirstType() {
		return one;
	}
	
	public Type getSecondType() {
		return two;
	}

}
