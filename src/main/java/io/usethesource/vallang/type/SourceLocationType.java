/*******************************************************************************
* Copyright (c) 2007, 2016 IBM Corporation, Centrum Wiskunde & Informatica
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package io.usethesource.vallang.type;

import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import io.usethesource.vallang.IConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;

/*package*/ final class SourceLocationType  extends DefaultSubtypeOfValue {
	private static final class InstanceKeeper {
      public final static SourceLocationType sInstance= new SourceLocationType();
    }

    public static SourceLocationType getInstance() {
        return InstanceKeeper.sInstance;
    }

    public static class Info implements TypeFactory.TypeReifier {

		@Override
		public Type getSymbolConstructorType() {
			return symbols().typeSymbolConstructor("loc");
		}

		@Override
		public Type fromSymbol(IConstructor symbol, TypeStore store,
                           Function<IConstructor, Set<IConstructor>> grammar) {
			return getInstance();
		}
		
		@Override
        public Type randomInstance(Supplier<Type> next, TypeStore store, Random rnd) {
            return tf().sourceLocationType();
        }

        public String randomLabel() {
            return null;
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
    return obj == SourceLocationType.getInstance();
}

    @Override
    public int hashCode() {
        return 61547;
    }

    @Override
    public String toString() {
        return "loc";
    }
    
    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
    	return visitor.visitSourceLocation(this);
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
      return type.isSubtypeOfSourceLocation(this);
    }
    
    @Override
    public Type lub(Type other) {
      return other.lubWithSourceLocation(this);
    }
    
    @Override
    public Type glb(Type type) {
      return type.glbWithSourceLocation(this);
    }
    
    @Override
    protected boolean isSubtypeOfSourceLocation(Type type) {
      return true;
    }
    
    @Override
    protected Type lubWithSourceLocation(Type type) {
      return this;
    }
    
    @Override
    protected Type glbWithSourceLocation(Type type) {
      return this;
    }
}
