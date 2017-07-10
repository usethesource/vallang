/*******************************************************************************
 * Copyright (c) 2009-2017 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   * Wietse Venema - wietsevenema@gmail.com - CWI
 *   * Jurgen Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Davy Landman - davy.landman@swat.engineering - SWAT.engineering
 *******************************************************************************/
package io.usethesource.vallang.random;

import java.util.Random;

import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public class RandomTypeGenerator {
    protected final TypeFactory tf = TypeFactory.getInstance();
    protected final Type[] atomicTypes;
    protected final Random random;

    public RandomTypeGenerator() {
        this(new Random());
    }
    
    public RandomTypeGenerator(Random random) {
        atomicTypes = new Type[] {
            tf.integerType(), tf.stringType(), tf.boolType(),
            tf.realType(), tf.integerType(), tf.rationalType(), tf.numberType(),
            tf.sourceLocationType(), tf.stringType(), tf.nodeType(), tf.boolType(), tf.dateTimeType()
        };
        this.random = random;
    }

    public Type next(int maxDepth) {
        if (maxDepth <= 0 || random.nextInt(atomicTypes.length + 4) < atomicTypes.length) {
            return getAtomicType();
        }
        return getRecursiveType(maxDepth - 1);
    }

    protected Type getRecursiveType(int maxDepth) {
        switch (random.nextInt(7)) { // tuples less often than the other type
            case 0:
            case 1: return tf.listType(next(maxDepth));
            case 2:
            case 3: return tf.setType(next(maxDepth));
            case 4:
            case 5: return tf.mapType(next(maxDepth), next(maxDepth));
            default: return getTupleType(maxDepth);
        }
    }

    protected Type getTupleType(int maxDepth) {
        Type[] args = new Type[1 + random.nextInt(Math.max(1, maxDepth))];
        for (int i = 0; i < args.length; i++) {
            args[i] = next(maxDepth - 1);
        }
        return tf.tupleType(args);
    }

    protected Type getAtomicType() {
        return atomicTypes[random.nextInt(atomicTypes.length)];
    }
}



