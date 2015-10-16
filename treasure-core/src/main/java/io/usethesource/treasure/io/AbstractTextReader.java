package io.usethesource.treasure.io;

import java.io.IOException;
import java.io.Reader;

import io.usethesource.treasure.IValue;
import io.usethesource.treasure.IValueFactory;
import io.usethesource.treasure.exceptions.FactTypeUseException;
import io.usethesource.treasure.type.Type;
import io.usethesource.treasure.type.TypeFactory;
import io.usethesource.treasure.type.TypeStore;

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
