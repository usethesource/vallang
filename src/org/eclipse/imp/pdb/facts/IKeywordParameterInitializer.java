package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.util.ImmutableMap;

public interface IKeywordParameterInitializer {
	ImmutableMap<String,IValue> initialize();
}
