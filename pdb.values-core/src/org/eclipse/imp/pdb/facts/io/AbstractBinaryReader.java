package org.eclipse.imp.pdb.facts.io;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;

public abstract class AbstractBinaryReader implements IValueBinaryReader {
	public IValue read(IValueFactory factory, Type type, InputStream stream)
			throws FactTypeUseException, IOException {
		return read(factory, new TypeStore(), type, stream);
	}

	public IValue read(IValueFactory factory, InputStream stream)
			throws FactTypeUseException, IOException {
		return read(factory, new TypeStore(), TypeFactory.getInstance().valueType(), stream);
	}
}
