package io.usethesource.vallang;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import io.usethesource.vallang.exceptions.FactTypeUseException;

public interface IWriter<T extends ICollection<T>> extends Iterable<IValue>, Collector<IValue, IWriter<T>, T> {
    /**
     * Modify this writer to insert only unique instances into the collection
     * @return
     */
    public default IWriter<T> unique() {
        return this;
    }

    /**
     * Insert several elements
     * @param value array of elements to insert
     */
    public void insert(IValue... value);

    /**
     * Append several elements
     * @param value array of elements to append
     */
    public default void append(IValue... value) {
        insert(value);
    }

    /**
     * Append elements at the end.
     *
     * @param value array of elements to append
     * @throws FactTypeUseException when done() was called before
     */
    public default void appendAll(Iterable<? extends IValue> collection) {
        for (IValue v : collection) {
            append(v);
        }
    }

    /**
     * Insert a tuple made of the given fields
     * @param fields
     */
    public void insertTuple(IValue... fields);

    /**
     * Append a tuple made of the given fields
     * @param fields
     */
    public default void appendTuple(IValue... fields) {
        insertTuple(fields);
    }

    /**
     * Inserts all elements of an iterable
     * @param collection
     */
    default void insertAll(Iterable<? extends IValue> collection) {
        for (IValue v : collection) {
            insert(v);
        }
    }

    public T done();


    @Override
    default BiConsumer<IWriter<T>, IValue> accumulator() {
        return (writer, elem) -> writer.append(elem);
    }

    @Override
    abstract Supplier<IWriter<T>> supplier();

    @Override
    default Function<IWriter<T>, T> finisher() {
        return w -> w.done();
    }

    @Override
    default BinaryOperator<IWriter<T>> combiner() {
        return (w1, w2) -> {
            w2.forEach(e -> w1.append(e));
            return w1;
        };
    }

    @Override
    default Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
