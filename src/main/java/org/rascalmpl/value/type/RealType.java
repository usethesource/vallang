/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and 2015 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *    Jurgen Vinju

 *******************************************************************************/

package org.rascalmpl.value.type;

import java.util.Set;
import java.util.function.Function;

import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.ISetWriter;
import org.rascalmpl.value.IValueFactory;

/*package*/ final class RealType extends NumberType {
	static final Type CONSTRUCTOR = declareTypeSymbol("real");

	private final static class InstanceKeeper {
		public final static RealType sInstance = new RealType();
	}

	@Override
	protected Type getReifiedConstructorType() {
		return CONSTRUCTOR;
	}

	public static Type fromSymbol(IConstructor symbol,TypeStore store, Function<IConstructor,Set<IConstructor>> grammar) {
		return TF.realType();
	}
	
	@Override
	public void asProductions(IValueFactory vf, TypeStore store, ISetWriter grammar, Set<IConstructor> done) {
		// TODO empty on purpose, pull up
	}
	 
	public static RealType getInstance() {
		return InstanceKeeper.sInstance;
	}

	/**
	 * Should never need to be called; there should be only one instance of
	 * IntegerType
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == RealType.getInstance();
	}

	@Override
	public int hashCode() {
		return 84121;
	}

	@Override
	public String toString() {
		return "real";
	}

	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitReal(this);
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
		return type.isSubtypeOfReal(this);
	}

	@Override
	public Type lub(Type type) {
		return type.lubWithReal(this);
	}

	@Override
	public Type glb(Type type) {
		return type.glbWithReal(this);
	}

	@Override
	protected Type lubWithReal(Type type) {
		return this;
	}

	@Override
	protected Type glbWithReal(Type type) {
		return this;
	}

	@Override
	protected Type glbWithNumber(Type type) {
		return this;
	}

	@Override
	protected Type glbWithRational(Type type) {
		return VoidType.getInstance();
	}

	@Override
	protected Type glbWithInteger(Type type) {
		return VoidType.getInstance();
	}

	@Override
	protected boolean isSubtypeOfReal(Type type) {
		return true;
	}
}
