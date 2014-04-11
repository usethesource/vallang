package org.eclipse.imp.pdb.facts;

import java.util.Map;

import org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableMap;
import org.eclipse.imp.pdb.facts.util.ImmutableMap;

public class ConstantKeywordParameterInitializer implements IKeywordParameterInitializer {
	private final Map<String, IValue> constants;
	
	public ConstantKeywordParameterInitializer(Map<String, IValue> constant) {
		this.constants = constant;
	}

	@Override
	public ImmutableMap<String, IValue> initialize() {
		return AbstractSpecialisedImmutableMap.mapOf(constants);
	}
}
