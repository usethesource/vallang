package org.eclipse.imp.pdb.facts.impl.hash;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IWriter;
import org.eclipse.imp.pdb.facts.type.FactTypeError;

public abstract class Writer implements IWriter {
	protected final HashMap<String,IValue> fAnnotations = new HashMap<String,IValue>();
	
	public void setAnnotation(String label, IValue value) throws FactTypeError {
		fAnnotations.put(label, value);
	}

	public void setAnnotations(Map<String, IValue> annotations)
			throws FactTypeError {
		fAnnotations.putAll(annotations);
	}
	
	public void insertAll(Iterable<IValue> collection) throws FactTypeError {
		for (IValue v : collection) {
			insert(v);
		}
	}
}
