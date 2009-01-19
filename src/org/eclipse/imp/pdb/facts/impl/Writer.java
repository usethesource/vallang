package org.eclipse.imp.pdb.facts.impl;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IWriter;
import org.eclipse.imp.pdb.facts.type.FactTypeError;

public abstract class Writer implements IWriter {
	public void insertAll(Iterable<IValue> collection) throws FactTypeError {
		for (IValue v : collection) {
			insert(v);
		}
	}
}
