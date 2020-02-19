/*******************************************************************************
 * Copyright (c) 2008 CWI.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jurgen Vinju - initial API and implementation
 *******************************************************************************/

package io.usethesource.vallang.type;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeReifier;



/**
 * A Parameter Type can be used to represent an abstract type,
 * i.e. a type that needs to be instantiated with an actual type
 * later.
 */
/*package*/ final class ParameterType extends DefaultSubtypeOfValue {
	private final String fName;
	private final Type fBound;

	/* package */ ParameterType(String name, Type bound) {
		fName = name.intern();
		fBound = bound;
	}

	/* package */ ParameterType(String name) {
		fName = name.intern();
		fBound = TypeFactory.getInstance().valueType();
	}

	public static class Info implements TypeReifier {
		@Override
		public Type getSymbolConstructorType() {
			return symbols().typeSymbolConstructor("parameter", tf().stringType() , "name", symbols().symbolADT(), "bound");
		}

		@Override
		public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor, Set<IConstructor>> grammar) {
			return tf().parameterType(((IString) symbol.get("name")).getValue(), symbols().fromSymbol((IConstructor) symbol.get("bound"), store, grammar));
		}

		@Override
		public Type randomInstance(Supplier<Type> next, TypeStore store, RandomTypesConfig rnd) {
		    if (rnd.isWithTypeParameters()) {
		        return tf().parameterType(randomLabel(rnd));
		    }
		    
		    return next.get();
		}
		
		@Override
		public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
				Set<IConstructor> done) {
			return vf.constructor(getSymbolConstructorType(), vf.string(type.getName()), type.getBound().asSymbol(vf, store, grammar, done));
		}

		@Override
		public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
				Set<IConstructor> done) {
			type.getBound().asProductions(vf, store, grammar, done);
		}
	}

	@Override
	public TypeReifier getTypeReifier() {
		return new Info();
	}

	@Override
	public Type getTypeParameters() {
	    return getBound().getTypeParameters();
	}
	
	@Override
	public Type getBound() {
		return fBound;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public int getArity(){
		return fBound.getArity();
	}

	@Override
	public Type getFieldType(int i){
		return fBound.getFieldType(i);
	}

	@Override
	public String[] getFieldNames(){
		return fBound.getFieldNames();
	}

	@Override
	public String toString() {
		return fBound.equivalent(ValueType.getInstance()) ? "&" + fName : "&" + fName + "<:" + fBound.toString();
	}

	@Override
	public int hashCode() {
		return 49991 + 49831 * fName.hashCode() + 133020331 * fBound.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object o) {
	    if (o == null) {
	        return false;
	    }
	    
		if (o instanceof ParameterType) {
			ParameterType other = (ParameterType) o;
			return fName.equals(other.fName) && fBound == other.fBound;
		}
		return false;
	}

	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitParameter(this);
	}

	/**
     * Read this as "could be instantiated as a super-type of"
     */
	@Override
	protected boolean isSupertypeOf(Type type) {
	    if (type == this) {
	        // here we assume hygenic type parameter binding
	        return true;
	    }
	    
	    // if the type parameter is totally free,
	    // it is bound by `value` only, then it can
	    // be a supertype of anything. This is also
	    // a stop condition for an infinite recursion.
	    if (getBound().isTop()) {
	        return true;
	    }
	    
	    // otherwise, the instantiated type goes no higher than the
	    // bound, so if the bound is not a supertype,
	    // then the parameter can't be either:
	    return getBound().isSupertypeOf(type);
	}

	@Override
	protected boolean isSubtypeOfAbstractData(Type type) {
	    return couldBeSubtypeOf(type);
	}

	/**
	 * @return true iff this parameter type when instantiated could
	 * be a sub-type of the given type.
	 */
    private boolean couldBeSubtypeOf(Type type) {
        // the only way that this is impossible if is the compared type
        // is not comparable to the bound
        return getBound().comparable(type);
    }
	
	@Override
	protected boolean isSubtypeOfBool(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfConstructor(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfDateTime(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfExternal(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfInteger(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfList(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfListRelation(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfMap(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfNode(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfNumber(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfParameter(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfRational(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfReal(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfRelation(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfSet(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfSourceLocation(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfString(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfTuple(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfValue(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	protected boolean isSubtypeOfVoid(Type type) {
	    return couldBeSubtypeOf(type);
	}
	
	@Override
	public Type lub(Type type) {
	    if (type == this) {
	        return this;
	    }
	    
	    if (type.isSubtypeOf(getBound())) {
            return this;
        }
        
	    return getBound().lub(type);
	}
	
	@Override
	public Type glb(Type type) {
	    if (type == this) {
	        return this;
	    }
	    
	    if (type.isSupertypeOf(getBound())) {
	        return this;
	    }
	    
	    return getBound().glb(type);
	}

	@Override
	public boolean match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		if (!super.match(matched, bindings)) {
			return false;
		}

		Type earlier = bindings.get(this);
		
		if (earlier != null) {
			Type lub = earlier.lub(matched);
			if (!lub.isSubtypeOf(getBound())) {
				return false;
			}

			bindings.put(this, lub);
			
			// propagate what we've learned about this parametertype
			// to possible open type parameters in its bound
			getBound().match(matched, bindings);
		}
		else {
			bindings.put(this, matched);
			getBound().match(matched, bindings);
		}

		return true;
	}

	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		Type result = bindings.get(this);

		if (result != null && result != this) {
		    try {
		        return result.instantiate(bindings);
		    }
		    catch (StackOverflowError e) {
		        assert false : "type bindings are not hygenic: cyclic parameter binding leads to infinite recursion";
		        return result;
		    }
		}
		else {
		    return TypeFactory.getInstance().parameterType(fName, getBound().instantiate(bindings));
		}
	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters,
	        int maxDepth, int maxWidth) {
	    IValue val = getBound().randomValue(random, vf, store, typeParameters, maxDepth, maxWidth);
	    
	    inferBinding(typeParameters, val);
	    
	    return val;
	}

    private void inferBinding(Map<Type, Type> typeParameters, IValue val) {
        Type tv = typeParameters.get(this);
	    
	    if (tv != null) {
	        tv = tv.lub(val.getType());
	    }
	    else {
	        tv = val.getType();
	    }
	    
	    typeParameters.put(this, tv);
    }
}
