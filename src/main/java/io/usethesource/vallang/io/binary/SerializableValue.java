package io.usethesource.vallang.io.binary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.io.binary.stream.IValueInputStream;
import io.usethesource.vallang.io.binary.stream.IValueOutputStream;
import io.usethesource.vallang.type.TypeStore;

/**
 * Experimental wrapper class for serializable IValues. When writing, it persists the class name of
 * value factory where a value origins form. When reading it reflectively instantiates the value
 * factory first before reading the value.
 *
 * NOTE: {@link #readObject(ObjectInputStream)} does allocate a new empty {@link TypeStore} instance
 * when reading in values.
 */
public class SerializableValue<T extends IValue> implements Serializable {
  private static final long serialVersionUID = -5507315290306212326L;

  private transient IValueFactory vf; // only class name is serialized
  private T value;

  /**
   * Wrapping a value factory and value tuple for serialization. 
   *
   * @param vf the value factory that produced {@link #value}
   * @param value the value to serialize
   */
  public SerializableValue(final IValueFactory vf, final T value) {
    this.vf = Objects.requireNonNull(vf);
    this.value = Objects.requireNonNull(value);
  }

  public T getValue() {
    return value;
  }

  public void write(OutputStream out) throws IOException {
    new ObjectOutputStream(out).writeObject(this);
  }

  @SuppressWarnings("unchecked")
  public static <U extends IValue> SerializableValue<U> read(InputStream in) throws IOException {
    try {
      return (SerializableValue<U>) new ObjectInputStream(in).readObject();
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    String factoryName = vf.getClass().getName();
    out.write("factory".getBytes());
    out.write(':');
    out.writeInt(factoryName.length());
    out.write(':');
    out.write(factoryName.getBytes("UTF8"));
    out.write(':');
    ByteArrayOutputStream bytesStream = new ByteArrayOutputStream();
    try (IValueOutputStream writer =
        new IValueOutputStream(bytesStream, vf, IValueOutputStream.CompressionRate.Normal)) {
      writer.write(value);
    }
    byte[] bytes = bytesStream.toByteArray();
    out.writeInt(bytes.length);
    out.write(':');
    out.write(bytes);
  }

  @SuppressWarnings("unchecked")
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    try {
      in.read(new byte["factory".length()], 0, "factory".length());
      in.read(); // ':'
      int length = in.readInt();
      in.read(); // ':'
      byte[] factoryName = new byte[length];
      in.read(factoryName, 0, length);
      in.read(); // ':'
      int amountOfBytes = in.readInt();
      in.read(); // ':'
      byte[] bytes = new byte[amountOfBytes];
      in.read(bytes);

      final Class<?> clazz = getClass().getClassLoader().loadClass(new String(factoryName, "UTF8"));
      this.vf = (IValueFactory) clazz.getMethod("getInstance").invoke(null);

      /**
       * NOTE: does allocate a new empty {@link TypeStore} instance when reading in values.
       */
      try (IValueInputStream reader =
          new IValueInputStream(new ByteArrayInputStream(bytes), vf, TypeStore::new)) {
        this.value = (T) reader.read();
      }
    } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException
        | NoSuchMethodException | SecurityException | ClassCastException e) {
      throw new IOException("Could not load IValueFactory", e);
    }
  }
}
