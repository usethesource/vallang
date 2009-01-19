package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.type.FactTypeError;

public interface IWriter {
	 void insert(IValue... value) throws FactTypeError;
	 void insertAll(Iterable<IValue> collection) throws FactTypeError;
	 public IValue done();
}
