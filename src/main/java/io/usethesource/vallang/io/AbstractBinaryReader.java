package io.usethesource.vallang.io;

import java.io.IOException;
import java.io.InputStream;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

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
