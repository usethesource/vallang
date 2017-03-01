package io.usethesource.vallang.io;

import java.io.IOException;
import java.io.Reader;

import io.usethesource.vallang.type.TypeStore;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

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
