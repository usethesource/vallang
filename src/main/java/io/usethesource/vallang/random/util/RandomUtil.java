package io.usethesource.vallang.random.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.UndeclaredAbstractDataTypeException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeStore;

public class RandomUtil {

    private interface StringGen {
        public void generate(Random rand, int length, StringBuilder result);
    }

    private static class CharRanges implements StringGen {
        private int[] start;
        private int[] stop;

        public CharRanges(int[] start, int[] stop) {
            assert start.length == stop.length;
            this.start = start;
            this.stop = stop;
        }

        public void generate(Random rand, int length, StringBuilder result) {
            for (int c = 0; c < length; c++) {
                int r = rand.nextInt(start.length);
                result.appendCodePoint(generateCodePoint(rand, start[r], stop[r]));
            }
        }

        private int generateCodePoint(Random rand, int start, int stop) {
            int range = stop - start;
            int result = 0;
            do {
                result = start + rand.nextInt(range + 1);
            } while (!validCodePoint(result));
            return result;
        }
    }

    private static class CharSets implements StringGen {
        private int[] chars;

        public CharSets(int... chars) {
            this.chars = chars;
        }

        @Override
        public void generate(Random rand, int length, StringBuilder result) {
            for (int c = 0; c < length; c++) {
                result.appendCodePoint(chars[rand.nextInt(chars.length)]);
            }
        }
    }

    private static class MixGenerators implements StringGen {
        private StringGen[] generators;

        public MixGenerators(StringGen... generators) {
            this.generators = generators;
        }
        @Override
        public void generate(Random rand, int length, StringBuilder result) {
            int left = length;
            while (left > 0) {
                int chunk = 1 + rand.nextInt(left);
                generators[rand.nextInt(generators.length)].generate(rand, chunk, result);
                left -= chunk;
            }
        }
    }

    private static boolean validCodePoint(int cp) {
        return Character.isDefined(cp)
            && Character.isValidCodePoint(cp)
            && Character.getType(cp) != Character.UNASSIGNED
            ;
    }

    private static String sanitize(String unclean) {
        // let's avoid testing with invalid codepoints
        int i = 0;
        char [] chars = unclean.toCharArray();
        while (i < chars.length) {
            char c = chars[i];
            if (Character.isHighSurrogate(c)) {
                i++;
                if (i < chars.length) {
                    int cp = Character.toCodePoint(c, chars[i]);
                    if (!validCodePoint(cp) || !Character.isSurrogatePair(c, chars[i])) {
                        chars[i-1]  = '_';
                        chars[i]    = '_';
                    }
                }
                else {
                    chars[i-1] = '_';
                }
            }
            else if (Character.isLowSurrogate(c)) {
                // this means the previous was not high
                chars[i] = '_';
            }
            else if (!validCodePoint(c)) {
                chars[i] = '_';
            }
            i++;
        }
        return new String(chars);
    }


    private static final StringGen alphaOnly = new CharRanges(new int[]{'a','A'}, new int[]{'z','Z'});
    private static final StringGen numeric = new CharRanges(new int[]{'0'}, new int[]{'9'});
    private static final StringGen generalStrangeChars = new CharRanges(new int[]{0x00, 0x21,0xA1}, new int[]{0x09,0x2F,0xAC});
    private static final StringGen normalUnicode = new CharRanges(new int[]{0x0100,0x3400,0xD000}, new int[]{0x0200,0x4D00,0xD700});
    private static final StringGen strangeUnicode = new CharRanges(new int[]{0x12000, 0x20000}, new int[]{0x1247F, 0x215FF});
    private static final StringGen whiteSpace = new CharSets(' ','\t','\n','\t');
    private static final StringGen strangeWhiteSpace = new CharSets(0x85, 0xA0, 0x1680, 0x2000, 0x2028, 0x2029,0x205F,0x3000);
    private static final StringGen rascalEscapes = new CharSets('\"','\'','>','\\','<','@','`');

    private static final StringGen[] generators = new StringGen[] {
        alphaOnly,
        new MixGenerators(alphaOnly, numeric),
        new MixGenerators(alphaOnly, numeric), // increase chances of normal strings
        numeric,
        normalUnicode,
        new MixGenerators(alphaOnly, numeric, generalStrangeChars),
        new MixGenerators(alphaOnly, numeric, whiteSpace),
        new MixGenerators(strangeWhiteSpace, whiteSpace),
        new MixGenerators(normalUnicode, strangeUnicode),
        new MixGenerators(alphaOnly, numeric, rascalEscapes),
        new MixGenerators(alphaOnly, numeric, generalStrangeChars, normalUnicode, whiteSpace, rascalEscapes)
    };

    public static boolean oneEvery(Random random, int n) {
        return random.nextInt(n) == 0;
    }

    public static String string(Random rand, int depth) {
        StringGen randomGenerator = generators[rand.nextInt(generators.length)];
        StringBuilder result = new StringBuilder(depth * 2);
        randomGenerator.generate(rand, depth, result);
        return sanitize(result.toString());
    }
    public static String stringAlphaNumeric(Random rand, int depth) {
        StringBuilder result = new StringBuilder(depth);
        new MixGenerators(alphaOnly, numeric).generate(rand, depth, result);
        return sanitize(result.toString());
    }

    public static String stringAlpha(Random rand, int depth) {
        StringBuilder result = new StringBuilder(depth);
        alphaOnly.generate(rand, depth, result);
        return sanitize(result.toString());
    }

    public static String stringNumeric(Random rand, int depth) {
        StringBuilder result = new StringBuilder(depth);
        numeric.generate(rand, depth, result);
        return sanitize(result.toString());
    }

    public static String stringAllKindsOfWhitespace(Random rand, int depth) {
        StringBuilder result = new StringBuilder(depth);
        new MixGenerators(whiteSpace, strangeWhiteSpace).generate(rand, depth, result);
        return sanitize(result.toString());
    }

    public static IValue randomADT(Type type, Random random, RandomTypesConfig typesConfig, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxWidth) {
        Type uninstantiatedADT = store.lookupAbstractDataType(type.getName());
        if (uninstantiatedADT == null) {
            throw new UndeclaredAbstractDataTypeException(type);
        }

        Map<Type,Type> bindings = new HashMap<>();
        uninstantiatedADT.match(type, bindings);

        Set<Type> candidates = store.lookupAlternatives(uninstantiatedADT);
        if (candidates.isEmpty()) {
            throw new RuntimeException("can not generate constructors for non-existing ADT: " + type);
        }

        Type constructor = pickRandom(random, candidates);

        if (maxDepth <= 0) {
            Type original = constructor;

            // find the constructor that does not add depth
            Iterator<Type> it = candidates.iterator();
            while (alwaysIncreasesDepth(constructor) && it.hasNext()) {
                constructor = it.next();
            }
            if (alwaysIncreasesDepth(constructor)) {
                constructor = original; // keep it random
            }
        }

        return generateConstructor(constructor, random, typesConfig, vf, store, bindings, maxDepth, maxWidth);
    }

    private static boolean alwaysIncreasesDepth(Type constructor) {
        for (int i = 0; i < constructor.getArity(); i++) {
            Type argType = constructor.getFieldType(i);
            if (argType.isAbstractData()) {
                return true;
            }
            if (argType.isTuple() && alwaysIncreasesDepth(argType)) {
                // tuple's can be increasing the depth.
                return true;
            }
        }
        return false;
    }

    private static Type pickRandom(Random random, Collection<Type> types) {
        int nth = random.nextInt(types.size());
        int index = 0;
        for (Type t: types) {
            if (index == nth) {
                return t;
            }
            index++;
        }
        throw new AssertionError("Dead code");
    }

    private static IValue generateConstructor(Type type, Random random, RandomTypesConfig typesConfig, IValueFactory vf, TypeStore store, Map<Type, Type> bindings, int maxDepth, int maxWidth) {
        Map<String, Type> kwParamsType = store.getKeywordParameters(type);

        if (type.getArity() == 0 && kwParamsType.size() == 0) {
            return vf.constructor(type);
        }

        IValue[] args = new IValue[type.getArity()];
        Type instantiatedConstructor = type.instantiate(bindings);
        for (int i = 0; i < args.length; i++) {
            args[i] = instantiatedConstructor.getFieldType(i).randomValue(random, typesConfig, vf, store, bindings, maxDepth - 1, maxWidth);
        }

        if (kwParamsType.size() > 0 && random.nextInt(3) == 0 && maxDepth > 0) {
            return vf.constructor(type, args, generateMappedArgs(kwParamsType, random, typesConfig, vf, store, bindings, maxDepth - 1, maxWidth));
        }

        return vf.constructor(type, args);
    }

    private static Map<String, IValue> generateMappedArgs(Map<String, Type> types, Random random, RandomTypesConfig typesConfig, IValueFactory vf, TypeStore store, Map<Type, Type> bindings, int maxDepth, int maxWidth) {
        Map<String, IValue> result = new HashMap<>();
        for (Entry<String,Type> tp : types.entrySet()) {
            if (random.nextBoolean()) {
                continue;
            }
            result.put(tp.getKey(), tp.getValue().randomValue(random, typesConfig, vf, store, bindings, maxDepth, maxWidth));
        }
        return result;
    }
}
