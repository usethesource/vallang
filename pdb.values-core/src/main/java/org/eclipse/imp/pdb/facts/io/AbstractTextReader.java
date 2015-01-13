package org.eclipse.imp.pdb.facts.io;

import java.io.IOException;
import java.io.Reader;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;

public abstract class AbstractTextReader implements IValueTextReader {
	public IValue read(IValueFactory factory, Type type, Reader reader)
			throws FactTypeUseException, IOException {
		return read(factory, new TypeStore(), type, reader);
	}

	public IValue read(IValueFactory factory, Reader reader)
			throws FactTypeUseException, IOException {
		return read(factory, new TypeStore(), TypeFactory.getInstance().valueType(), reader);
	}
}
