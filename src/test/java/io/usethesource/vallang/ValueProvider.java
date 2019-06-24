package io.usethesource.vallang;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import io.usethesource.vallang.random.RandomValueGenerator;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

/**
 * This value provider generates automatically/randomly values for test parameters of type:
 *    IValueFactory
 *    TypeFactory
 *    TypeStore
 *    IValue
 *    IList
 *    ISet
 *    IMap
 *    IInteger
 *    IReal
 *    INumber
 *    IRational
 *    INode
 *    IConstructor
 *    ITuple
 *    ISourceLocation
 *    
 *    If the class under test has a static field called "store" of type TypeStore, then this
 *    typestore will be passed to all parameters of type TypeStore instead of a fresh/empty TypeStore.
 */
public class ValueProvider implements ArgumentsProvider {
    private static final Random rnd = new Random();
    private static final boolean enableAnnotations = true;
    private static final TypeFactory tf = TypeFactory.getInstance();

    /**
     * Every vallang test is run using all implementations of IValueFactory.
     */
    private static final IValueFactory[] factories = { 
            io.usethesource.vallang.impl.reference.ValueFactory.getInstance(),  
            io.usethesource.vallang.impl.persistent.ValueFactory.getInstance()
            };
    
    /**
     * The random value generator is parametrized by the valuefactory at creation time.
     * We need to keep the reference due get better randomized results between (re-runs of) 
     * individual tests. 
     */
    private static final RandomValueGenerator[] generators = {
            new RandomValueGenerator(factories[0], rnd, 5, 10, enableAnnotations),
            new RandomValueGenerator(factories[1], rnd, 5, 10, enableAnnotations)
    };
    
    /**
     * This trivial class helps with streaming generated test inputs, and some other stuff.
     */
    private static class Tuple<A,B> {
        public A a;
        public B b;
        
        public Tuple(A a, B b) {
            this.a = a;
            this.b = b;
        }
        
        public static <C,D> Tuple<C,D> of(C c, D d) {
            return new Tuple<>(c, d);
        }
    }
    
    /**
     * Maps Java class literals of sub-types of IValue to the corresponding function which will
     * generate a (random) instance of a type that all instances of such Java classes could have.
     * Only composite types will actually be random.
     */
    private static final Map<Class<? extends IValue>, BiFunction<TypeStore, ExpectedType, Type>> types = 
        Stream.<Tuple<Class<? extends IValue>, BiFunction<TypeStore, ExpectedType, Type>>>of(
            Tuple.of(IInteger.class,        (ts, n) -> tf.integerType()),
            Tuple.of(IBool.class,           (ts, n) -> tf.boolType()),
            Tuple.of(IReal.class,           (ts, n) -> tf.realType()),
            Tuple.of(IRational.class,       (ts, n) -> tf.rationalType()),
            Tuple.of(INumber.class,         (ts, n) -> tf.numberType()),
            Tuple.of(IString.class,         (ts, n) -> tf.stringType()),
            Tuple.of(ISourceLocation.class, (ts, n) -> tf.sourceLocationType()),
            Tuple.of(IValue.class,          (ts, n) -> tf.valueType()),
            Tuple.of(INode.class,           (ts, n) -> tf.nodeType()),
            Tuple.of(IList.class,           (ts, n) -> tf.listType(tf.randomType())),
            Tuple.of(ISet.class,            (ts, n) -> tf.setType(tf.randomType())),
            Tuple.of(ITuple.class,          (ts, n) -> tf.tupleType(tf.randomType(), tf.randomType())),
            Tuple.of(IMap.class,            (ts, n) -> tf.mapType(tf.randomType(), tf.randomType())),
            Tuple.of(IConstructor.class,    (ts, n) -> randomADT(ts, n))
        ).collect(Collectors.toMap(t -> t.a, t -> t.b));
            
    
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        Method method = context.getTestMethod().get();
        
        /*
         * If only factories and typestores are arguments, we generate as many tests as we have
         * value factory implementations (2). For the IValue argument we generate 100 tests and for
         * every additional IValue argument we multiply the number of tests by 10.
         */
        long valueArity = Arrays.stream(method.getParameterTypes()).filter(x -> IValue.class.isAssignableFrom(x)).count();
        int numberOfTests = Math.max(1, 100 * (int) Math.pow(10, valueArity - 1));
        
        return Stream.of(
                   Tuple.of(factories[0], generators[0]), // every factory has its own generator
                   Tuple.of(factories[1], generators[1])
               ).flatMap(vf ->                            // all parameters share the same factory
                   generateTypeStore(context).flatMap(ts ->
                       Stream.iterate(arguments(method, vf, ts), p -> arguments(method, vf, ts)).limit(numberOfTests)
                   ) 
               );
    }

    private static Type randomADT(TypeStore ts, ExpectedType n) {
        if (n != null) {
            Type adt = ts.lookupAbstractDataType(n.value());
            
            if (adt != null) {
                return adt;
            }
            else {
                throw new IllegalArgumentException(n.value() + " is not declared by the given TypeStore");
            }
        }
        
        Collection<Type> allADTs = ts.getAbstractDataTypes();

        if (!allADTs.isEmpty()) {
            return allADTs.stream().skip(new Random().nextInt(allADTs.size())).findFirst().get();
        }

        // note the side-effect in the type store!
        Type x = tf.abstractDataType(ts, "X");
        tf.constructor(ts, x, "x");

        return x;
    }

    /**
     * Generate the random argument for a single test method
     * @param method the declaration of the method under test
     * @param vf        the valuefactory to use when generating values, also passed to parameters of type IValueFactory
     * @param ts        the TypeStore to request ADTs from, randomly, also passed to parameters of type TypeStore
     * @return an Arguments instance for streaming into JUnits MethodSource interface.
     */
    private Arguments arguments(Method method, Tuple<IValueFactory, RandomValueGenerator> vf, TypeStore ts) {
        return Arguments.of(Arrays.stream(method.getParameters()).map(cl -> argument(vf, ts, cl.getType(), cl.getAnnotation(ExpectedType.class))).toArray());    
    }
    
    /**
     * Generate an argument to a vallang test function. `cls` can be any sub-type of IValue,
     * or TypeStore or IValueFactory.
     * @param vf        the valuefactory to use when generating values, also passed to parameters of type IValueFactory
     * @param ts        the TypeStore to request ADTs from, randomly, also passed to parameters of type TypeStore
     * @param cls       the class type of the parameter to generate an input for
     * @return a random object which is assignable to cls
     */
    private Object argument(Tuple<IValueFactory, RandomValueGenerator> vf, TypeStore ts, Class<?> cls, ExpectedType name)  {
        if (cls.isAssignableFrom(IValueFactory.class)) {
            return vf.a;
        }
        else if (cls.isAssignableFrom(TypeStore.class)) {
            return ts;
        }
        else if (cls.isAssignableFrom(TypeFactory.class)) {
            return TypeFactory.getInstance();
        }
        else if (IValue.class.isAssignableFrom(cls)) {
            return generateValue(vf, ts, cls.asSubclass(IValue.class), name);
        }
        else {
            throw new IllegalArgumentException(cls + " is not assignable from IValue, IValueFactory, TypeStore or TypeFactory");
        }
    }
    
    /**
     * Generate a random IValue instance
     * 
     * @param vf  the valuefactory/randomgenerator to use
     * @param ts  the TypeStore to draw ADT constructors from
     * @param cl  the `cl` (sub-type of `IValue`) to be assignable to
     * @return an instance assignable to `cl`
     */
    private IValue generateValue(Tuple<IValueFactory, RandomValueGenerator> vf, TypeStore ts, Class<? extends IValue> cl, ExpectedType name) {
        return vf.b.generate(types.getOrDefault(cl, (x, n) -> tf.valueType()).apply(ts, name), ts, Collections.emptyMap());
    }
    
    /**
     * Generates a TypeStore instance by importing the static `store` field of the class-under-test (if-present)
     * in a fresh TypeStore. Otherwise it generates a fresh and empty TypeStore.  
     * @param context
     * @return
     */
    private Stream<TypeStore> generateTypeStore(ExtensionContext context) {
        try {
            return Stream.of(new TypeStore((TypeStore) context.getRequiredTestClass().getField("store").get("null")));
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            return Stream.of(new TypeStore());
        }
    }
}