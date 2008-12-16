package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class BoolValue extends Value implements IBool {
	private final boolean fValue;

	/*package*/ BoolValue(boolean b) {
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
		if (getClass() == obj.getClass()) {
			return equalAnnotations((Value) obj) && fValue == ((BoolValue) obj).fValue;
		}
		return false;
	}
	
	public IBool and(IBool other) {
		return new BoolValue(fValue && other.getValue());
	}
	
	public IBool or(IBool other) {
		return new BoolValue(fValue || other.getValue());
	}
	
	public IBool not() {
		return new BoolValue(!fValue);
	}
	
	public IBool implies(IBool other) {
		return new BoolValue(fValue ? other.getValue() : true);
	}
	
	public IBool equivalent(IBool other) {
		return new BoolValue(fValue == other.getValue());
	}
	
	public IBool xor(IBool other) {
		return new BoolValue(fValue ^ other.getValue());
	}
	
	@Override
	public int hashCode() {
		return fAnnotations.hashCode() << 8 + (fValue ? 1231 : 1237);
	}
}
