package io.usethesource.vallang;

import io.usethesource.vallang.exceptions.FactTypeUseException;

public interface IWriter {
	 void insert(IValue... value) throws FactTypeUseException;
	 void insertAll(Iterable<? extends IValue> collection) throws FactTypeUseException;
	 public IValue done();
}
