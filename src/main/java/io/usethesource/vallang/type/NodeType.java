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

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.type.TypeFactory.RandomTypesConfig;
import io.usethesource.vallang.type.TypeFactory.TypeReifier;

/**
 * A type for values that are nodes. All INode have the type NodeType, and all
 * IConstructors have NodeType as a supertype.
 */
class NodeType extends DefaultSubtypeOfValue {
	protected static class InstanceKeeper {
		public final static NodeType sInstance = new NodeType();
	}

	public static NodeType getInstance() {
		return InstanceKeeper.sInstance;
	}

	public static class Info implements TypeReifier {
		@Override
		public Type getSymbolConstructorType() {
			return symbols().typeSymbolConstructor("node");
		}

		@Override
		public Type fromSymbol(IConstructor symbol, TypeStore store,
				Function<IConstructor, Set<IConstructor>> grammar) {
			return getInstance();
		}
		
		@Override
		public Type randomInstance(Supplier<Type> next, RandomTypesConfig rnd) {
		    return tf().nodeType();
		}
	}

	@Override
	public TypeReifier getTypeReifier() {
		return new Info();
	}


	@Override
	public String toString() {
		return "node";
	}

	/**
	 * Should never be called, NodeType is a singleton
	 */
	@Override
	public boolean equals(@Nullable Object o) {
		return o == NodeType.getInstance();
	}

	@Override
	public int hashCode() {
		return 20102;
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
		return type.isSubtypeOfNode(this);
	}

	@Override
	public Type lub(Type other) {
		return other.lubWithNode(this);
	}

	@Override
	protected boolean isSubtypeOfNode(Type type) {
		return true;
	}

	@Override
	protected Type lubWithAbstractData(Type type) {
		return this;
	}

	@Override
	protected Type lubWithConstructor(Type type) {
		return this;
	}

	@Override
	protected Type lubWithNode(Type type) {
		return type;
	}

	@Override
	public Type glb(Type type) {
		return type.glbWithNode(this);
	}

	@Override
	protected Type glbWithNode(Type type) {
		return this;
	}

	@Override
	protected Type glbWithConstructor(Type type) {
		return type;
	}

	@Override
	protected Type glbWithAbstractData(Type type) {
		return type;
	}

	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitNode(this);
	}
}
