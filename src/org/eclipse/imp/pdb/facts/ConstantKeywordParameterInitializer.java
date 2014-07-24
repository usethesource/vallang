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
}
