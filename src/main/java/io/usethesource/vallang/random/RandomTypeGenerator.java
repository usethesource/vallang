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
    private final TypeFactory tf = TypeFactory.getInstance();
    private final Type[] atomicTypes;
    private final Random random;

    public RandomTypeGenerator() {
        this(new Random());
    }
    public RandomTypeGenerator(Random random) {
        atomicTypes = new Type[] {
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

    private Type getRecursiveType(int maxDepth) {
        switch (random.nextInt(4)) {
            case 0: return tf.listType(next(maxDepth));
            case 1: return tf.setType(next(maxDepth));
            case 2: return tf.mapType(next(maxDepth), next(maxDepth));
            default: return getTupleType(maxDepth);
        }
    }

    private Type getTupleType(int maxDepth) {
        Type[] args = new Type[Math.max(1, random.nextInt(maxDepth))];
        for (int i = 0; i < args.length; i++) {
            args[i] = next(maxDepth - 1);
        }
        return tf.tupleType(args);
    }

    private Type getAtomicType() {
        return atomicTypes[random.nextInt(atomicTypes.length)];
    }
}



