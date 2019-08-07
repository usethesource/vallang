/** 
 * Copyright (c) 2017, Davy Landman, Centrum Wiskunde & Informatica (CWI) 
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package io.usethesource.vallang.random;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.random.util.RandomUtil;
import io.usethesource.vallang.type.ITypeVisitor;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A generator of RandomValues, based on Wietse Venema's Cobra generator in the rascal project
 * @author Davy Landman
 *
 */
public class RandomValueGenerator implements ITypeVisitor<IValue, RuntimeException> {

    protected final IValueFactory vf;
    protected final Random random;
    protected final int maxDepth;
    protected final RandomTypeGenerator rt;
    protected final boolean generateAnnotations;
    protected final int maxWidth;
    
    protected int currentDepth;
    protected @Nullable TypeStore currentStore;
    protected @Nullable Map<Type, Type> typeParameters;
    
    

    public RandomValueGenerator(IValueFactory vf, Random random, int maxDepth, int maxWidth, boolean generateAnnotations) {
        if (maxDepth <= 0) {
            throw new IllegalArgumentException("maxDepth is supposed to be 1 or higher");
        }
        this.vf = vf;
        this.random = random;
        this.maxDepth = maxDepth;
        this.maxWidth = maxWidth;
        this.generateAnnotations = generateAnnotations;
        this.rt = new RandomTypeGenerator(random);

        this.currentStore = null;
        this.currentDepth = -1;
        this.typeParameters = null;
    }
    
    public RandomValueGenerator setAnnotations(boolean gen) {
        return new RandomValueGenerator(vf, random, maxDepth, maxWidth, gen);
    }
    
    public Random getRandom() {
        return random;
    }
    
    /**
     * Generate a new random value of a given type
     * @param type which type to generate
     * @param store corresponding reified type store that contains the type and all constructors available in this type
     * @param typeParameters a map of the bound type parameters
     * @return a new IValue corresponding to this type
     */
    public IValue generate(Type type, TypeStore store, Map<Type, Type> typeParameters) {
        if (currentDepth != -1 || currentStore != null || this.typeParameters != null) {
            throw new IllegalStateException("Don't call this method in a nested scenario, RandomValueGenerator's should only be re-used sequentually");
        }
        currentDepth = 0;
        currentStore = store;
        this.typeParameters = typeParameters;
        try {
            return type.accept(this);
        } finally {
            currentDepth = -1;
            currentStore = null;
            this.typeParameters = null;
        }
    }
    
    protected IValue continueGenerating(Type tp) {
        return tp.accept(this);
    }
    protected IValue generateOneDeeper(Type tp) {
        try {
            currentDepth++;
            return tp.accept(this);
        }
        finally {
            currentDepth--;
        }
    }
    
    @Override
    public IValue visitReal(Type type) throws RuntimeException {
        if (oneEvery(5)) {
            return vf.real(10 * random.nextDouble());
        }
        if (oneEvery(5)) {
            return vf.real(-10 * random.nextDouble());
        }
        if (oneEvery(10)) {
            return vf.real(random.nextDouble());
        }
        if (oneEvery(10)) {
            return vf.real(-random.nextDouble());
        }

        if (oneEvery(20) && depthLeft() > 1) {
            BigDecimal r = BigDecimal.valueOf(random.nextDouble()).multiply(BigDecimal.valueOf(random.nextInt(10000)));
            r = r.multiply(BigDecimal.valueOf(random.nextInt()).add(BigDecimal.valueOf(1000)));
            return vf.real(r.toString());
        }
        
        return vf.real(0.0);
    }

    protected int depthLeft() {
        return maxDepth - currentDepth;
    }

    @Override
    public IValue visitInteger(Type type) throws RuntimeException {
        if (oneEvery(5) && depthLeft() > 1) {
            return vf.integer(random.nextInt());
        }
        if (oneEvery(5)) {
            return vf.integer(random.nextInt(10));
        }
        if (oneEvery(5)) {
            return vf.integer(-random.nextInt(10));
        }
        if (oneEvery(20) && depthLeft() > 1) {
            // sometimes, a very huge number
            IInteger result = vf.integer(random.nextLong());
            do {
                result = result.multiply(vf.integer(random.nextLong()));
            }
            while (random.nextFloat() > 0.4);
            return result.add(vf.integer(random.nextInt()));
        }
        return vf.integer(0);
    }

    @Override
    public IValue visitRational(Type type) throws RuntimeException {
        int b = 0;
        while (b == 0) {
            b = random.nextInt();
        }
        return vf.rational(random.nextInt(), b);
    }
    
    @Override
    public IValue visitNumber(Type type) throws RuntimeException {
        switch (random.nextInt(3)) {
            case 0:
                return this.visitInteger(type);
            case 1:
                return this.visitReal(type);
            default:
                return this.visitRational(type);
        }
    }

    @Override
    public IValue visitBool(Type type) throws RuntimeException {
        return vf.bool(random.nextBoolean());
    }

    @Override
    public IValue visitDateTime(Type type) throws RuntimeException {
        Calendar cal = Calendar.getInstance();
        try {
            int milliOffset = random.nextInt(1000) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.MILLISECOND, milliOffset);
            int second = random.nextInt(60) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.SECOND, second);
            int minute = random.nextInt(60) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.MINUTE, minute);
            int hour = random.nextInt(60) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.HOUR_OF_DAY, hour);
            int day = random.nextInt(30) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.DAY_OF_MONTH, day);
            int month = random.nextInt(12) * (random.nextBoolean() ? -1 : 1);
            cal.roll(Calendar.MONTH, month);

            // make sure we do not go over the 4 digit year limit, which breaks things
            int year = random.nextInt(5000) * (random.nextBoolean() ? -1 : 1);

            // make sure we don't go into negative territory
            if (cal.get(Calendar.YEAR) + year < 1)
                cal.add(Calendar.YEAR, 1);
            else
                cal.add(Calendar.YEAR, year);

            return vf.datetime(cal.getTimeInMillis());
        }
        catch (IllegalArgumentException e) {
            // this may happen if the generated random time does
            // not exist due to timezone shifting or due to historical
            // calendar standardization changes
            // So, we just try again until we hit a better random date
            return visitDateTime(type);
            // of continued failure before we run out of stack are low.
        }
    }
    
    private boolean oneEvery(int n) {
        return random.nextInt(n) == 0;
    }

    @Override
    public IValue visitSourceLocation(Type type) throws RuntimeException {
        try {
            String scheme = RandomUtil.stringAlpha(random, 1 + random.nextInt(Math.max(1, depthLeft())));;
            String authority = "";
            String path = "";
            String query = "";
            String fragment = "";
            
            while (!oneEvery(3) && depthLeft() > 1) {
                path += "/"  + (random.nextDouble() < 0.9 ? RandomUtil.stringAlphaNumeric(random, 1 + random.nextInt(5)) : RandomUtil.string(random, 1 + random.nextInt(5)));
            }
            if (path.isEmpty()) {
                path = "/";
            }
            
            if (oneEvery(4)) {
                authority = RandomUtil.stringAlphaNumeric(random, 1 + random.nextInt(6));
            }
            
            if (oneEvery(30) && depthLeft() > 1) {
                while (!oneEvery(3)) {
                    if (!query.isEmpty()) {
                        query += "&";
                    }
                    query += RandomUtil.stringAlpha(random, 1 + random.nextInt(4)) + "=" + RandomUtil.stringAlphaNumeric(random, 1 + random.nextInt(4));
                }
            }
            
            if (oneEvery(30) && depthLeft() > 1) {
                fragment = RandomUtil.stringAlphaNumeric(random, 1 + random.nextInt(5));
            }
            
            return vf.sourceLocation(scheme, authority, path, query, fragment);
        } catch (URISyntaxException e) {
            // generated illegal URI?
            try {
                return vf.sourceLocation("tmp", "", "/");
            }
            catch (URISyntaxException e1) {
                throw new RuntimeException("fallback source location should always be correct");
            }
        }
    }

    @Override
    public IValue visitString(Type type) throws RuntimeException {
        if (random.nextBoolean() || currentDepth >= maxDepth) {
            return vf.string("");
        }
        return vf.string(RandomUtil.string(random, 1 + random.nextInt(depthLeft() + 3)));
    }


    @Override
    public IValue visitList(Type type) throws RuntimeException {
        IListWriter result = vf.listWriter();
        if (currentDepth < maxDepth && random.nextBoolean()) {
            int size = Math.min(maxWidth, 1 + random.nextInt(depthLeft()));
            for (int i =0; i < size; i++) {
                result.append(generateOneDeeper(type.getElementType()));
            }
        }
        return result.done();
    }

    @Override
    public IValue visitMap(Type type) throws RuntimeException {
        IMapWriter result = vf.mapWriter();
        if (currentDepth < maxDepth && random.nextBoolean()) {
            int size = Math.min(maxWidth, 1 + random.nextInt(depthLeft()));
            for (int i =0; i < size; i++) {
                result.put(generateOneDeeper(type.getKeyType()),generateOneDeeper(type.getValueType()));
            }
        }
        return result.done();
    }

    @Override
    public IValue visitSet(Type type) throws RuntimeException {
        ISetWriter result = vf.setWriter();
        if (currentDepth < maxDepth && random.nextBoolean()) {
            int size = Math.min(maxWidth, 1 + random.nextInt(depthLeft()));
            for (int i =0; i < size; i++) {
                result.insert(generateOneDeeper(type.getElementType()));
            }
        }
        return result.done();
    }

    @Override
    public IValue visitTuple(Type type) throws RuntimeException {
        IValue[] elems = new IValue[type.getArity()];
        for (int i = 0; i < elems.length; i++) {
            elems[i] = generateOneDeeper(type.getFieldType(i));
        }
        return vf.tuple(elems);
    }


    @Override
    public IValue visitNode(Type type) throws RuntimeException {
        String name = random.nextBoolean() ? RandomUtil.string(random, 1 + random.nextInt(5)) : RandomUtil.stringAlpha(random, random.nextInt(5));

        int arity = currentDepth >= maxDepth ? 0 : random.nextInt(depthLeft());
        IValue[] args = new IValue[arity];
        for (int i = 0; i < arity; i++) {
            args[i] = generateOneDeeper(TypeFactory.getInstance().valueType());
        }

        if (oneEvery(4) && currentDepth < maxDepth) {
            int kwArity = 1 + random.nextInt(depthLeft());
            Map<String, IValue> kwParams = new HashMap<>(kwArity);
            for (int i = 0; i < kwArity; i++) {
                String kwName = "";
                while (kwName.isEmpty()) {
                    // names have to start with alpha character
                    kwName = RandomUtil.stringAlpha(random, 3); 
                }
                kwName += RandomUtil.stringAlphaNumeric(random, 4);
                kwParams.put(kwName, generateOneDeeper(TypeFactory.getInstance().valueType()));
            }
            // normally they are kw params, but sometimes they are annotations
            if (oneEvery(10)) {
                return vf.node(name, kwParams, args);
            }
            return vf.node(name, args, kwParams);
        }
        return vf.node(name, args);	
    }

    private Type pickRandom(Collection<Type> types) {
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

    /**
     * Find out if the arguments to the constructor contain ADTs or if they contain types that don't increase nesting
     */
    protected boolean alwaysIncreasesDepth(Type constructor) {
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

    @Override
    public IValue visitAbstractData(Type type) throws RuntimeException {
        TypeStore store = currentStore;
        if (store == null) {
            throw new RuntimeException("Missing TypeStore");
        }
        Set<Type> candidates = store.lookupAlternatives(type);
        if (candidates.isEmpty()) {
            throw new UnsupportedOperationException("The "+type+" ADT has no constructors in the type store");
        }
        Type constructor = pickRandom(candidates);
        if (depthLeft() <= 0) {
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
        return continueGenerating(constructor);
    }


    @SuppressWarnings("deprecation")
    @Override
    public IValue visitConstructor(Type type) throws RuntimeException {
        TypeStore store = currentStore;
        if (store == null) {
            throw new RuntimeException("Missing TypeStore");
        }
        Map<String, Type> kwParamsType = store.getKeywordParameters(type);
        Map<String, Type> annoType = store.getAnnotations(type);
        
        if (type.getArity() == 0 && kwParamsType.size() == 0 && annoType.size() == 0) { 
            return vf.constructor(type);
        } 
        
        IValue[] args = new IValue[type.getArity()];
        for (int i = 0; i < args.length; i++) {
            args[i] = generateOneDeeper(type.getFieldType(i));
        }
        
        if (kwParamsType.size() > 0 && oneEvery(3) && depthLeft() > 0) {
            return vf.constructor(type, args, generateMappedArgs(kwParamsType));
        }

        if (generateAnnotations && annoType.size() > 0 && oneEvery(5) && depthLeft() > 0) {
            return vf.constructor(type, generateMappedArgs(annoType), args);
        }

        return vf.constructor(type, args);
    }



    private Map<String, IValue> generateMappedArgs(Map<String, Type> types) {
        Map<String, IValue> result = new HashMap<>();
        for (Entry<String,Type> tp : types.entrySet()) {
            if (random.nextBoolean()) {
                continue;
            }
            result.put(tp.getKey(), generateOneDeeper(tp.getValue()));
        }
        return result;
    }

    @Override
    public IValue visitValue(Type type) throws RuntimeException {
        return continueGenerating(rt.next(depthLeft()));
    }

    @Override
    public IValue visitParameter(Type parameterType) throws RuntimeException {
        Type type = Objects.requireNonNull(typeParameters, "Type Parameters not set").get(parameterType);
        if(type == null){
            throw new IllegalArgumentException("Unbound type parameter " + parameterType);
        }
        return continueGenerating(type);
    }

    @Override
    public IValue visitAlias(Type type) throws RuntimeException {
        return continueGenerating(type.getAliased());
    }

    @Override
    public IValue visitExternal(Type type) throws RuntimeException {
        throw new RuntimeException("External type (" + type + ") not supported, use inheritance to add it");
    }
    @Override
    public IValue visitVoid(Type type) throws RuntimeException {
        throw new RuntimeException("Void has no values");
    }
}
