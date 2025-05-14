package io.usethesource.vallang;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import io.usethesource.vallang.exceptions.FactParseError;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.io.StandardTextReader;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeStore;

/**
 * <p>This value provider generates automatically/randomly values for test parameters of type:
 *    <ul><li>IValueFactory</li>
 *    <li>TypeFactory</li>
 *    <li>TypeStore</li>
 *    <li>IValue</li>
 *    <li>IList</li>
 *    <li>ISet</li>
 *    <li>IMap</li>
 *    <li>IInteger</li>
 *    <li>IReal</li>
 *    <li>INumber</li>
 *    <li>IRational</li>
 *    <li>INode</li>
 *    <li>IConstructor</li>
 *    <li>ITuple</li>
 *    <li>ISourceLocation</li>
 *    <li>IDateTime</li>
 *    <li>Type</li></ul></p>
 *
 *    <p>If the class under test has a static field called "store" of type TypeStore, then this
 *    typestore will be passed to all parameters of type TypeStore instead of a fresh/empty TypeStore.</p>
 *
 *   <p>If a parameter of a method under test is annotated with \@ExpectedType("type") like so:
 *      <pre>\@ParameterizedTest \@ArgumentsSource(ValueProvider.class)
 *      public void myTest(\@ExpectedType("set[int]") ISet set) ...</pre>
 *
 *   , then the ValueProvider will generate only instances which have as run-time type a
 *   sub-type of the specified expected type.</p>
 *
 *  <p>
 *  If a method under test is annotated with \@ArgumentsSeed(long) then that seed is used to
 *  generate the stream of argument lists for the given method</p>
 *
 *  <p>If a parameter of a method under test is annotate with \@GivenValue("expression") then
 *  instead of a random parameter, the value expression is parsed as an IValue and passed as given parameter.
 *  The parser respects the current TypeStore as well as optional \@ExpectedType annotations on the same parameter.</p>
 *
 *  <p>If a parameter of a method under test of type {@link Type} is annotated with \@TypeConfig then the
 *  random type generator is configurated using that annotation. For example:
 *  \@TypeConfig(Option.All) will activate type aliases, open type parameters and field names for tuples.</p>
 *
 *  <p>If a method under test is annotated with \@ArgumentsMaxDepth(int), then none of the random parameters
 *  will be nested deeper than the given number. \@ArgumentsMaxWidth(int) has a similar meaning but for the width
 *  of tuples, lists, maps, and sets.</p>
 */
public class ValueProvider implements ArgumentsProvider {
    private static final TypeFactory tf = TypeFactory.getInstance();

    private static final @Nullable String seedProperty;
    private static final long seed;
    private static final Random rnd;

    static {
        seedProperty = System.getProperty("vallang.test.seed");
        if (seedProperty != null) {
            System.err.println("Current random seed is computed from -Dvallang.test.seed=" + seedProperty);
            seed = hashSeed(seedProperty);
            rnd = new Random(seed);
        }
        else {
            seed = new Random().nextLong();
            rnd = new Random(seed);
        }

        System.err.println("Current random seed is: " + seed);
    }

    /**
     * We use this to accidentally generate arguments which are the same as the previous
     * once in a while:
     */
    private IValue previous = null;

    /**
     * Every vallang test is run using all implementations of IValueFactory.
     */
    private static final IValueFactory[] factories = {
            io.usethesource.vallang.impl.reference.ValueFactory.getInstance(),
            io.usethesource.vallang.impl.persistent.ValueFactory.getInstance()
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

    @FunctionalInterface
    private interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    /**
     * Maps Java class literals of sub-types of IValue to the corresponding function which will
     * generate a (random) instance of a type that all instances of such Java classes could have.
     * Only composite types will actually be random.
     */
    private static final Map<Class<? extends IValue>, TriFunction<TypeStore, ExpectedType, RandomTypesConfig, Type>> types =
        Stream.<Tuple<Class<? extends IValue>, TriFunction<TypeStore, ExpectedType, RandomTypesConfig, Type>>>of(
            Tuple.of(IInteger.class,        (ts, n, rtc) -> tf.integerType()),
            Tuple.of(IDateTime.class,       (ts, n, rtc) -> tf.dateTimeType()),
            Tuple.of(IBool.class,           (ts, n, rtc) -> tf.boolType()),
            Tuple.of(IReal.class,           (ts, n, rtc) -> tf.realType()),
            Tuple.of(IRational.class,       (ts, n, rtc) -> tf.rationalType()),
            Tuple.of(INumber.class,         (ts, n, rtc) -> tf.numberType()),
            Tuple.of(IString.class,         (ts, n, rtc) -> tf.stringType()),
            Tuple.of(ISourceLocation.class, (ts, n, rtc) -> tf.sourceLocationType()),
            Tuple.of(IValue.class,          (ts, n, rtc) -> tf.valueType()),
            Tuple.of(INode.class,           (ts, n, rtc) -> tf.nodeType()),
            Tuple.of(IList.class,           (ts, n, rtc) -> tf.listType(tf.randomType(ts, rtc))),
            Tuple.of(ISet.class,            (ts, n, rtc) -> tf.setType(tf.randomType(ts, rtc))),
            Tuple.of(ITuple.class,          (ts, n, rtc) -> tf.tupleType(tf.randomType(ts, rtc), tf.randomType(ts, rtc))),
            Tuple.of(IMap.class,            (ts, n, rtc) -> tf.mapType(tf.randomType(ts, rtc), tf.randomType(ts, rtc))),
            Tuple.of(IConstructor.class,    (ts, n, rtc) -> randomADT(ts, n, rtc))
        ).collect(Collectors.toMap(t -> t.a, t -> t.b));


    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
        Method method = context.getTestMethod().get();

        tf.abstractDataType(null,"bla").randomType(null, null)
        /*
         * If only factories and typestores are arguments, we generate as many tests as we have
         * value factory implementations (2). For the IValue argument we generate 100 tests and for
         * every additional IValue argument we multiply the number of tests by 10.
         */
        long valueArity = Arrays.stream(method.getParameterTypes()).filter(x -> IValue.class.isAssignableFrom(x) || Type.class.isAssignableFrom(x)).count()
                - Arrays.stream(method.getParameters()).filter(x -> x.getAnnotation(GivenValue.class) != null).count();
        int numberOfTests = Math.max(1, 100 * (int) Math.pow(10, valueArity - 1));

        ArgumentsSeed argSeed = method.getAnnotation(ArgumentsSeed.class);
        if (argSeed != null) {
            rnd.setSeed(argSeed.value());
        }
        else {
            rnd.setSeed(seed);
        }

        return Stream.of(
                factories[0],
                factories[1]
               ).flatMap(vf ->                            // all parameters share the same factory
                   generateTypeStore(context).flatMap(ts ->
                       Stream.iterate(arguments(method, vf, ts), p -> arguments(method, vf, ts)).limit(numberOfTests)
                   )
               );
    }

    private static Type randomADT(TypeStore ts, ExpectedType n, RandomTypesConfig rtc)  {
        if (n != null) {
            Type result = readType(ts, n);
            if (result != null) {
                return result;
            }
        }

        Collection<Type> allADTs = ts.getAbstractDataTypes();

        if (!allADTs.isEmpty()) {
            return allADTs.stream().skip(new Random().nextInt(allADTs.size())).findFirst().get();
        }

        return TypeFactory.getInstance().randomADTType(ts, rtc);
    }

    /**
     * Generate the random argument for a single test method
     * @param method the declaration of the method under test
     * @param vf        the valuefactory to use when generating values, also passed to parameters of type IValueFactory
     * @param ts        the TypeStore to request ADTs from, randomly, also passed to parameters of type TypeStore
     * @return an Arguments instance for streaming into JUnits MethodSource interface.
     */
    private Arguments arguments(Method method, IValueFactory vf, TypeStore ts) {
        previous = null; // never reuse arguments from a previous instance


        ArgumentsMaxDepth depth = method.getAnnotation(ArgumentsMaxDepth.class);
        ArgumentsMaxWidth width = method.getAnnotation(ArgumentsMaxWidth.class);

        TypeStore tsp = new TypeStore();
        tsp.extendStore(ts);

        return Arguments.of(
                Arrays.stream(method.getParameters()).map(
                        cl -> argument(
                                vf,
                                tsp,
                                cl.getType(),
                                cl.getAnnotation(ExpectedType.class),
                                cl.getAnnotation(GivenValue.class),
                                first(method.getAnnotation(TypeConfig.class), cl.getAnnotation(TypeConfig.class)),
                                depth != null ? depth.value() : 5,
                                width != null ? width.value() : 10
                                )).toArray().clone()
                );
    }

    private TypeConfig first(TypeConfig first, TypeConfig second) {
        if (first != null) {
            return first;
        }
        return second;
    }

    private static long hashSeed(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31*h + string.charAt(i);
        }
        return h;
    }


    /**
     * Generate an argument to a vallang test function. `cls` can be any sub-type of IValue,
     * or TypeStore or IValueFactory.
     * @param vf        the valuefactory to use when generating values, also passed to parameters of type IValueFactory
     * @param ts        the TypeStore to request ADTs from, randomly, also passed to parameters of type TypeStore
     * @param cls       the class type of the parameter to generate an input for
     * @return a random object which is assignable to cls
     */
    private Object argument(IValueFactory vf, TypeStore ts, Class<?> cls, @Nullable ExpectedType expected, GivenValue givenValue, TypeConfig typeConfig, int depth, int width)  {
        if (givenValue != null) {
            try {
                if (expected != null) {
                    Type type = readType(ts, expected);
                    if (type != null) {
                        return new StandardTextReader().read(vf, ts, type, new StringReader(givenValue.value()));
                    }
                }
                return new StandardTextReader().read(vf, new StringReader(givenValue.value()));
            } catch (FactTypeUseException | IOException e) {
                System.err.println("[WARNING] failed to parse given value: " + givenValue.value());
            }
        }

        if (cls.isAssignableFrom(IValueFactory.class)) {
            return vf;
        }
        else if (cls.isAssignableFrom(TypeStore.class)) {
            return ts;
        }
        else if (cls.isAssignableFrom(Type.class)) {
            if (expected != null) {
                Type result = readType(ts, expected);
                if (result != null) {
                    return result;
                }
            }
            RandomTypesConfig rtc = configureRandomTypes(typeConfig, depth);
            return TypeFactory.getInstance().randomType(ts, rtc);
        }
        else if (cls.isAssignableFrom(TypeFactory.class)) {
            return TypeFactory.getInstance();
        }
        else if (IValue.class.isAssignableFrom(cls)) {
            RandomTypesConfig rtc = configureRandomTypes(typeConfig, depth);
            return generateValue(vf, ts, cls.asSubclass(IValue.class), expected, depth, width, rtc);
        }
        else if (Random.class.isAssignableFrom(cls)) {
            return rnd;
        }
        else {
            throw new IllegalArgumentException(cls + " is not assignable from IValue, IValueFactory, TypeStore or TypeFactory");
        }
    }

    private RandomTypesConfig configureRandomTypes(TypeConfig typeConfig, int depth) {
        RandomTypesConfig tc = RandomTypesConfig.defaultConfig(rnd).maxDepth(depth);

        if (typeConfig != null) {
            for (TypeConfig.Option p : typeConfig.value()) {
                switch (p) {
                    case ALIASES:
                        tc = tc.withAliases();
                        break;
                    case TUPLE_FIELDNAMES:
                        tc = tc.withTupleFieldNames();
                        break;
                    case TYPE_PARAMETERS:
                        tc = tc.withTypeParameters();
                        break;
                    case MAP_FIELDNAMES:
                        tc = tc.withMapFieldNames();
                        break;
                    case ALL:
                        tc = tc.withAliases().withTupleFieldNames().withTypeParameters();
                        break;
                }
            }
        }

        return tc;
    }

    /**
     * Generate a random IValue instance
     *
     * @param vf  the valuefactory/randomgenerator to use
     * @param ts  the TypeStore to draw ADT constructors from
     * @param cl  the `cl` (sub-type of `IValue`) to be assignable to
     * @param noAnnotations
     * @return an instance assignable to `cl`
     */
    private IValue generateValue(IValueFactory vf, TypeStore ts, Class<? extends IValue> cl, @Nullable ExpectedType expected, int depth, int width, RandomTypesConfig rtc) {
        Type expectedType = tf.voidType();


        // this should terminate through random selection.
        // only tuple types with nested void arguments can reduce to void.
        int i = 0;
        while (expectedType.isBottom() && i++ < 1000) {
            if (expected != null) {
                Type read = readType(ts, expected);
                if (read == null) {
                    expected = null;
                }
                else {
                    expectedType = read;
                    break;
                }
            }
            else {
                expectedType = types
                    .getOrDefault(cl, (x, n, rt) -> tf.valueType())
                    .apply(ts, expected, rtc);
            }
        }

        assert !expectedType.isBottom() : cl + " generated void type?";

        // here we duplicate the previous parameter if it was compatible with the current expected type
        // this greatly enhances coverage of code under test, if matching or equality testing play a role.
        if (previous != null && rnd.nextInt(4) == 0 && previous.getType().isSubtypeOf(expectedType)) {
            // 50% of the time we serialize and parse the repeated value to get a different object instance
            // that is still ".equals"
            return rnd.nextBoolean() ? previous : reinstantiate(vf, ts, previous);
        }

        return (previous = expectedType.randomValue(rnd, vf, ts, new HashMap<>(), depth, width));
    }

    private static @Nullable Type readType(TypeStore ts, ExpectedType expected) {
        try {
            return tf.fromString(ts, new StringReader(expected.value()));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Produces a value which equals the input `val` but is not the same object reference.
     * It does this by serializing the value and parsing it again with the same expected type.
     * @return a value equals to `val` (val.equals(returnValue)) but not reference equal (val != returnValue)
     */
    private IValue reinstantiate(IValueFactory vf, TypeStore ts, IValue val) {
        try {
            return new StandardTextReader().read(vf, ts, val.getType(), new StringReader(val.toString()));
        } catch (FactTypeUseException | FactParseError | IOException e) {
            System.err.println("WARNING: value reinstantation via serialization failed for ["+val+"] because + \""+e.getMessage()+"\". Reusing reference.");
            return val;
        }
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
