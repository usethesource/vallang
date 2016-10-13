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

package org.rascalmpl.value.type;

import java.util.Set;
import java.util.function.Function;

import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.type.TypeFactory.TypeReifier;

/*package*/ final class RationalType extends NumberType {
	private static final class InstanceKeeper {
		public final static RationalType sInstance= new RationalType();
	}

	public static RationalType getInstance() {
		return InstanceKeeper.sInstance;
	}

	public static class Info implements TypeReifier {

		@Override
		public Type getSymbolConstructorType() {
			return symbols().typeSymbolConstructor("rat");
		}

		@Override
		public Type fromSymbol(IConstructor symbol, TypeStore store,
				Function<IConstructor, Set<IConstructor>> grammar) {
			return getInstance();
		}
	}
	
	@Override
	public TypeReifier getTypeReifier() {
		return new Info();
	}

	/**
	 * Should never need to be called; there should be only one instance of IntegerType
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == RationalType.getInstance();
	}

	@Override
	public int hashCode() {
		return 212873;
	}

	@Override
	public String toString() {
		return "rat";
	}

	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitRational(this);
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
		return type.isSubtypeOfRational(this);
	}

	@Override
	public Type lub(Type other) {
		return other.lubWithRational(this);
	}

	@Override
	protected Type lubWithRational(Type type) {
		return this;
	}

	@Override
	protected boolean isSubtypeOfRational(Type type) {
		return true;
	}

	@Override
	public Type glb(Type type) {
		return type.glbWithRational(this);
	}

	@Override
	protected Type glbWithNumber(Type type) {
		return this;
	}

	@Override
	protected Type glbWithRational(Type type) {
		return this;
	}

	@Override
	protected Type glbWithReal(Type type) {
		return VoidType.getInstance();
	}

	protected Type glbWithInteger(Type type) {
		return VoidType.getInstance();
	}
}
