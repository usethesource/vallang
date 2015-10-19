package org.rascalmpl.value;

import org.rascalmpl.value.exceptions.FactTypeUseException;

public interface IWriter {
	 void insert(IValue... value) throws FactTypeUseException;
	 void insertAll(Iterable<? extends IValue> collection) throws FactTypeUseException;
	 public IValue done();
}
