package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

public class BoolValue extends Value implements IBool {
	private final boolean fValue;

	public BoolValue(boolean b) {
		super(TypeFactory.getInstance().boolType());
		this.fValue = b;
	}
	
	private BoolValue(BoolValue other, String label, IValue anno) {
		super(other, label, anno);
    	fValue = other.fValue;
	}

	@Override
	protected IValue clone(String label, IValue value) {
		return new BoolValue(this, label, value);
	}

	public boolean getValue() {
		return fValue;
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitBoolean(this);
	}
	
	@Override
	public String toString() {
		return fValue ? "true" : "false";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BoolValue) {
			return fValue == ((BoolValue) obj).fValue;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return fValue ? 1 : 3;
	}
}
