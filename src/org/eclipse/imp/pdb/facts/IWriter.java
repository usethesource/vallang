package org.eclipse.imp.pdb.facts;

import java.util.Map;

import org.eclipse.imp.pdb.facts.type.FactTypeError;

public interface IWriter {
	 void insert(IValue... value) throws FactTypeError;
	 void insertAll(Iterable<IValue> collection) throws FactTypeError;
	 void setAnnotation(String label, IValue value) throws FactTypeError;
	 void setAnnotations(Map<String,IValue> annotations) throws FactTypeError;
	 public IValue done();
}
