package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IWriter;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

public abstract class Writer implements IWriter {
	public void insertAll(Iterable<IValue> collection) throws FactTypeUseException {
		for (IValue v : collection) {
			insert(v);
		}
	}
}
