package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.util.ImmutableMap;

public interface IKeywordParameterInitializer {
	IValue initialize(ImmutableMap<String,IValue> environment);
}
