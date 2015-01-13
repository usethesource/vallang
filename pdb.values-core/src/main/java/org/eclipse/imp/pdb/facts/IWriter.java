package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

public interface IWriter {
	 void insert(IValue... value) throws FactTypeUseException;
	 void insertAll(Iterable<? extends IValue> collection) throws FactTypeUseException;
	 public IValue done();
}
