/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package io.usethesource.vallang.type;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.random.util.RandomUtil;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;

import org.checkerframework.checker.nullness.qual.Nullable;

/*package*/ final class StringType extends DefaultSubtypeOfValue {
	private static final class InstanceKeeper {
      private final static StringType sInstance= new StringType();
    }

    public static StringType getInstance() {
        return InstanceKeeper.sInstance;
    }

    public static class Info implements TypeFactory.TypeReifier {

		@Override
		public Type getSymbolConstructorType() {
			return symbols().typeSymbolConstructor("str");
		}

		@Override
		public Type fromSymbol(IConstructor symbol, TypeStore store,
                           Function<IConstructor, Set<IConstructor>> grammar) {
			return getInstance();
		}
		
		@Override
        public Type randomInstance(Supplier<Type> next, TypeStore store, RandomTypesConfig rnd) {
            return tf().stringType();
        }
	}
    
    @Override
	public TypeFactory.TypeReifier getTypeReifier() {
		return new Info();
	}
    
    /**
     * Should never need to be called; there should be only one instance of IntegerType
     */
    @Override
    public boolean equals(@Nullable Object obj) {
    return obj == StringType.getInstance();
}

    @Override
    public int hashCode() {
        return 94903;
    }

    @Override
    public String toString() {
        return "str";
    }
    
    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
    	return visitor.visitString(this);
    }
    
    @Override
    protected boolean isSupertypeOf(Type type) {
      return type.isSubtypeOfString(this);
    }
    
    @Override
    public Type lub(Type other) {
      return other.lubWithString(this);
    }
    
    @Override
    public Type glb(Type type) {
      return type.glbWithString(this);
    }
    
    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithString(this);
    }
    
    @Override
    protected boolean intersectsWithString(Type type) {
        return true;
    }
    
    @Override
    protected boolean isSubtypeOfString(Type type) {
      return true;
    }
    
    @Override
    protected Type lubWithString(Type type) {
      return this;
    }
    
    @Override
    protected Type glbWithString(Type type) {
      return this;
    }
    
    @Override
    public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxWidth) {
        if (random.nextBoolean() || maxDepth <= 0) {
            return vf.string("");
        }
        
        return vf.string(RandomUtil.string(random, 1 + random.nextInt(maxDepth + 3)));
    }
    
    @Override
    public boolean isString() {
        return true;
    }
}
