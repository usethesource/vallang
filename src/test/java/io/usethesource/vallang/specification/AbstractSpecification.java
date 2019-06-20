package io.usethesource.vallang.specification;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.INumber;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.random.RandomValueGenerator;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public class AbstractSpecification {
    protected static class ValueProvider implements ArgumentsProvider {
        private static final TypeFactory tf = TypeFactory.getInstance();
        private static final IValueFactory referenceFactory = io.usethesource.vallang.impl.reference.ValueFactory.getInstance();
        private static final IValueFactory persistentFactory = io.usethesource.vallang.impl.persistent.ValueFactory.getInstance();
        private static final TypeStore store = new TypeStore();
        private static final Random rnd = new Random();
        private static final RandomValueGenerator referenceGen = new RandomValueGenerator(referenceFactory, rnd, 5, 10, true);
        private static final RandomValueGenerator persistentGen = new RandomValueGenerator(persistentFactory, rnd, 5, 10, true);
        private static final int MAX = 100;
        
        private static final Map<Class<?>, Type> types = Stream.of(
                new AbstractMap.SimpleEntry<Class<?>, Type>(IInteger.class, tf.integerType()),
                new AbstractMap.SimpleEntry<Class<?>, Type>(IReal.class, tf.realType()),
                new AbstractMap.SimpleEntry<Class<?>, Type>(IRational.class, tf.rationalType()),
                new AbstractMap.SimpleEntry<Class<?>, Type>(INumber.class, tf.numberType()),
                new AbstractMap.SimpleEntry<Class<?>, Type>(IString.class, tf.stringType()),
                new AbstractMap.SimpleEntry<Class<?>, Type>(ISourceLocation.class, tf.sourceLocationType()),
                new AbstractMap.SimpleEntry<Class<?>, Type>(IValue.class, tf.valueType()),
                new AbstractMap.SimpleEntry<Class<?>, Type>(INode.class, tf.nodeType()),
                new AbstractMap.SimpleEntry<Class<?>, Type>(IConstructor.class, tf.integerType()),
                new AbstractMap.SimpleEntry<Class<?>, Type>(IList.class, tf.listType(tf.valueType())),
                new AbstractMap.SimpleEntry<Class<?>, Type>(ISet.class, tf.setType(tf.valueType())),
                new AbstractMap.SimpleEntry<Class<?>, Type>(IMap.class, tf.mapType(tf.valueType(), tf.valueType()))
        ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            Method method = context.getTestMethod().get();
            
            // we generate as many values as there are IValue parameters
            return Stream.generate(() -> Arguments.of(
                    Arrays.stream(method.getParameterTypes()).map(
                            cl -> {
                                if (cl.isAssignableFrom(IValueFactory.class)) {
                                    return rnd.nextBoolean() ? referenceFactory : persistentFactory;
                                } else {
                                    return rnd.nextBoolean() ? referenceValue(cl) : persistentValue(cl);
                                }
                            }
                            ).toArray()
                    )).limit(MAX);
        }

        private IValue persistentValue(Class<?> cl) {
            return persistentGen.generate(types.getOrDefault(cl, tf.valueType()), store, Collections.emptyMap());
        }

        private IValue referenceValue(Class<?> cl) {
            return referenceGen.generate(types.getOrDefault(cl, tf.valueType()), store, Collections.emptyMap());
        }
    }
}