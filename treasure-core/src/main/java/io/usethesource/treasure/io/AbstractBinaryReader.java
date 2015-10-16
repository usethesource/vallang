package io.usethesource.treasure.io;

import java.io.IOException;
import java.io.InputStream;

import io.usethesource.treasure.IValue;
import io.usethesource.treasure.IValueFactory;
import io.usethesource.treasure.exceptions.FactTypeUseException;
import io.usethesource.treasure.type.Type;
import io.usethesource.treasure.type.TypeFactory;
import io.usethesource.treasure.type.TypeStore;

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
