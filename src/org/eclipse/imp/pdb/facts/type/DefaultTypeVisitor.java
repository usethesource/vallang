package org.eclipse.imp.pdb.facts.type;

public abstract class DefaultTypeVisitor<T,E extends Throwable> implements ITypeVisitor<T, E> {
  protected final T def;

  public DefaultTypeVisitor(T def) {
    this.def = def;
  }

  @Override
  public T visitReal(Type type) throws E {
    return def;
  }

  @Override
  public T visitInteger(Type type) throws E {
    return def;
  }

  @Override
  public T visitRational(Type type) throws E {
    return def;
  }

  @Override
  public T visitList(Type type) throws E {
    return def;
  }

  @Override
  public T visitMap(Type type) throws E {
    return def;
  }

  @Override
  public T visitNumber(Type type) throws E {
    return def;
  }

  @Override
  public T visitAlias(Type type) throws E {
    return def;
  }

  @Override
  public T visitSet(Type type) throws E {
    return def;
  }

  @Override
  public T visitSourceLocation(Type type) throws E {
    return def;
  }

  @Override
  public T visitString(Type type) throws E {
    return def;
  }

  @Override
  public T visitNode(Type type) throws E {
    return def;
  }

  @Override
  public T visitConstructor(Type type) throws E {
    return def;
  }

  @Override
  public T visitAbstractData(Type type) throws E {
    return def;
  }

  @Override
  public T visitTuple(Type type) throws E {
    return def;
  }

  @Override
  public T visitValue(Type type) throws E {
    return def;
  }

  @Override
  public T visitVoid(Type type) throws E {
    return def;
  }

  @Override
  public T visitBool(Type type) throws E {
    return def;
  }

  @Override
  public T visitParameter(Type type) throws E {
    return def;
  }

  @Override
  public T visitExternal(Type type) throws E {
    return def;
  }

  @Override
  public T visitDateTime(Type type) throws E {
    return def;
  }
}
