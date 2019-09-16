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

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.usethesource.vallang.random.util.RandomUtil.oneEvery;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.random.util.RandomUtil;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;

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
        public Type randomInstance(Supplier<Type> next, TypeStore store, RandomTypesConfig rnd) {
            return tf().sourceLocationType();
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
    
    @Override
    public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
            int maxDepth, int maxWidth) {
        try {
            String scheme = RandomUtil.stringAlpha(random, 1 + random.nextInt(Math.max(1, maxDepth)));;
            String authority = "";
            String path = "";
            String query = "";
            String fragment = "";

            while (!oneEvery(random, 3) && maxDepth > 0) {
                path += "/"  + (random.nextDouble() < 0.9 ? RandomUtil.stringAlphaNumeric(random, 1 + random.nextInt(5)) : RandomUtil.string(random, 1 + random.nextInt(5)));
            }
            if (path.isEmpty()) {
                path = "/";
            }

            if (oneEvery(random, 4)) {
                authority = RandomUtil.stringAlphaNumeric(random, 1 + random.nextInt(6));
            }

            if (oneEvery(random, 30) && maxDepth > 0) {
                while (!oneEvery(random, 3)) {
                    if (!query.isEmpty()) {
                        query += "&";
                    }
                    query += RandomUtil.stringAlpha(random, 1 + random.nextInt(4)) + "=" + RandomUtil.stringAlphaNumeric(random, 1 + random.nextInt(4));
                }
            }

            if (oneEvery(random, 30) && maxDepth > 0) {
                fragment = RandomUtil.stringAlphaNumeric(random, 1 + random.nextInt(5));
            }

            ISourceLocation result = vf.sourceLocation(scheme, authority, path, query, fragment);
            if (oneEvery(random, 10) && maxDepth > 0) {
                try {
                    // also generate offset and line and friends
                    int bound = oneEvery(random, 10) ? Integer.MAX_VALUE : 512;
                    if (oneEvery(random, 3)) {
                        int startLine = random.nextInt(bound);
                        int endLine = Math.addExact(startLine, random.nextInt(bound));
                        result = vf.sourceLocation(result, random.nextInt(bound), random.nextInt(bound), startLine,
                                endLine, random.nextInt(bound), random.nextInt(bound));
                    } else {
                        result = vf.sourceLocation(result, random.nextInt(bound), random.nextInt(bound));
                    }
                }
                catch (IllegalArgumentException | ArithmeticException ignored) {
                    // something went wrong with generating the the offset, so let's just fall back to returning the
                    // plain result without added offsets
                }
            }
            return result;
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
}
