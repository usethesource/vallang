package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class RedeclaredFieldNameException extends
		FactTypeDeclarationException {
	private static final long serialVersionUID = -4401062690006714904L;
	private String label;
	private Type one;
	private Type two;
	private Type tupleType;
	
	public RedeclaredFieldNameException(String label, Type one, Type two, Type tupleType) {
		super("The field name " + label + " is illegally used for both " + one + " and " + two + " in type " + tupleType);
		this.label = label;
		this.one = one;
		this.two = two;
		this.tupleType = tupleType;
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
	
	public Type getTupleType() {
		return tupleType;
	}

}
