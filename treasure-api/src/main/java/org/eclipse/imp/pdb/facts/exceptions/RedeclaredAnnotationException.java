package org.eclipse.imp.pdb.facts.exceptions;

import org.eclipse.imp.pdb.facts.type.Type;

public class RedeclaredAnnotationException extends FactTypeDeclarationException {
	private static final long serialVersionUID = -2118606173620347920L;
	private String label;
	private Type earlier;

	public RedeclaredAnnotationException(String label, Type earlier) {
		super("Annotation " + label + " was declared earlier as " + earlier);
		this.label = label;
		this.earlier = earlier;
	}
	
	public String getLabel() {
		return label;
	}
	
	public Type getEarlierType() {
		return earlier;
	}
}
