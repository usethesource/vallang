package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

public class BoolValue extends Value implements IBool {
	private final boolean value;

	public BoolValue(boolean b) {
		super(TypeFactory.getInstance().boolType());
		this.value = b;
	}
	
	private BoolValue(BoolValue other) {
		super(other);
    	value = other.value;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return new BoolValue(this);
	}

	public boolean getValue() {
		return value;
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitBoolean(this);
	}
	
	@Override
	public String toString() {
		return value ? "true" : "false";
	}

}
