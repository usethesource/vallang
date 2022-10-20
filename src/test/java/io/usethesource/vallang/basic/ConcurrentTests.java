package io.usethesource.vallang.basic;

import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.ValueProvider;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public class ConcurrentTests {
    
    @ParameterizedTest @ArgumentsSource(ValueProvider.class)
    public void parallelRandomGenerators(IValueFactory vf) throws InterruptedException, BrokenBarrierException, TimeoutException {
        int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
        var allStarted = new CyclicBarrier(cores + 1);
        var allDone = new CyclicBarrier(cores + 1);
        var error = new AtomicReference<Exception>(null);
        for (int i = 0; i < cores; i++) {
            var runner = new Thread(() -> {
                try {
                    Random r = new Random();
                    r.setSeed(Thread.currentThread().getId());
                    TypeStore ts = new TypeStore();

                    allStarted.await();
                    // now we run this init sequence at the same time, hoping to crash something
                    TypeFactory tf = TypeFactory.getInstance();
                    Type Boolean = tf.abstractDataType(ts, "Boolean");

                    Type Name = tf.abstractDataType(ts, "Name");
                    Type True = tf.constructor(ts, Boolean, "true");
                    Type False = tf.constructor(ts, Boolean, "false");
                    Type And = tf.constructor(ts, Boolean, "and", Boolean, Boolean);
                    Type Or = tf.constructor(ts, Boolean, "or", tf.listType(Boolean));
                    Type Not = tf.constructor(ts, Boolean, "not", Boolean);
                    Type TwoTups = tf.constructor(ts, Boolean, "twotups", tf.tupleType(Boolean, Boolean),
                        tf.tupleType(Boolean, Boolean));
                    Type NameNode = tf.constructor(ts, Name, "name", tf.stringType());
                    Type Friends = tf.constructor(ts, Boolean, "friends", tf.listType(Name));
                    Type Couples = tf.constructor(ts, Boolean, "couples", tf.listType(tf.tupleType(Name, Name)));
                    ts.declareKeywordParameter(Name, "moreName", Name);
                    ts.declareKeywordParameter(Name, "listName", tf.listType(Name));
                    ts.declareKeywordParameter(Name, "anyValue", tf.valueType());
                    for (int j = 0; j < 1000; j++) {
                        tf.valueType().randomValue(r, vf, ts, null, 5, 5);
                    }
                    allDone.await();
                }
                catch (Exception failure) {
                    failure.printStackTrace();
                    error.set(failure);
                }
                finally {
                    try {
                        allDone.await(1, TimeUnit.SECONDS); // just to be sure, if nobody is waiting, we just stop.
                    } catch (InterruptedException | BrokenBarrierException | TimeoutException e) {
                        //ignore ;
                    }
                }

            });
            runner.setDaemon(true);
            runner.start();
        }

        allStarted.await();
        allDone.await(10, TimeUnit.MINUTES);
        assertNull(error.get(), "Should be no exception when running init");
    }
}
