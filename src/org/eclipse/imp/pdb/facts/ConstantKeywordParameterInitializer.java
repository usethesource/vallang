package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.util.ImmutableMap;

public class ConstantKeywordParameterInitializer implements IKeywordParameterInitializer {
	private final IValue constant;
	
	public ConstantKeywordParameterInitializer(IValue constant) {
		this.constant = constant;
	}

	@Override
	public IValue initialize(ImmutableMap<String,IValue> arguments) {
		return constant;
	}
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConstantKeywordParameterInitializer)) {
			return false;
		}
		return ((ConstantKeywordParameterInitializer)obj).constant.isEqual(constant);
	}
	@Override
	public int hashCode() {
		return constant.hashCode();
	}
	@Override
	public String toString() {
		return constant.toString();
	}
}
