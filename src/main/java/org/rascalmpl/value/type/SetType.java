/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation, Centrum Wiskunde & Informatica
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *    Jurgen Vinju (Jurgen.Vinju@cwi.nl)
 *******************************************************************************/

package org.rascalmpl.value.type;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.IList;
import org.rascalmpl.value.ISetWriter;
import org.rascalmpl.value.IValueFactory;
import org.rascalmpl.value.exceptions.FactTypeUseException;
import org.rascalmpl.value.type.TypeFactory.TypeReifier;

/*package*/class SetType extends DefaultSubtypeOfValue {
	protected final Type fEltType;

	/* package */SetType(Type eltType) {
		fEltType = eltType;
	}

	public static class Info implements TypeReifier {
		@Override
		public Type getSymbolConstructorType() {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public Set<Type> getSymbolConstructorTypes() {
			return Arrays.stream(new Type[] { 
					getSetType(),
					getRelType() // TODO: can be removed after bootstrap
			}).collect(Collectors.toSet());
		}
		
		@Override
		public Type randomInstance(Supplier<Type> next, TypeStore store, Random rnd) {
		    return tf().setType(next.get());
		}
		
		@Override
		public boolean isRecursive() {
		    return false;
		}

		private Type getRelType() {
			return symbols().typeSymbolConstructor("rel", tf().listType(symbols().symbolADT()), "symbols");
		}

		private Type getSetType() {
			return symbols().typeSymbolConstructor("set", symbols().symbolADT(), "symbol");
		}

		@Override
		public Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor, Set<IConstructor>> grammar) {
			if (symbol.getConstructorType() == getSetType()) {
				return tf().setType(symbols().fromSymbol((IConstructor) symbol.get("symbol"), store, grammar));
			}
			else {
				// TODO remove; this is for bootstrapping with an old version
				return tf().setType(symbols().fromSymbols((IList) symbol.get("symbols"), store, grammar));
			}
		}

		@Override
		public IConstructor toSymbol(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
				Set<IConstructor> done) {
			return vf.constructor(getSetType(), type.getElementType().asSymbol(vf, store, grammar, done));
		}
		
		@Override
		public void asProductions(Type type, IValueFactory vf, TypeStore store, ISetWriter grammar,
				Set<IConstructor> done) {
			type.getElementType().asProductions(vf, store, grammar, done);
		}

        public String randomLabel() {
            return null;
        }
	}
	

	@Override
	public TypeReifier getTypeReifier() {
		return new Info();
	}

	@Override
	public Type getElementType() {
		return fEltType;
	}

	@Override
	public boolean hasFieldNames() {
		return fEltType.hasFieldNames();
	}

	@Override 
	public boolean hasField(String fieldName) {
		return fEltType.hasField(fieldName);
	}

	@Override
	public int getFieldIndex(String fieldName) {
		return fEltType.getFieldIndex(fieldName);
	}

	@Override
	public Type getFieldType(int i) {
		return fEltType.getFieldType(i);
	}

	@Override
	public String getFieldName(int i) {
		return fEltType.getFieldName(i);
	}

	@Override
	public String[] getFieldNames() {
		return fEltType.getFieldNames();
	}

	@Override
	public Type getFieldType(String fieldName) throws FactTypeUseException {
		return fEltType.getFieldType(fieldName);
	}

	@Override
	public Type getFieldTypes() {
		return fEltType.getFieldTypes();
	}

	@Override
	public int getArity() {
		return fEltType.getArity();
	}

	@Override
	public Type carrier() {
		return fEltType.carrier();
	}

	@Override
	public Type closure() {
		return TF.setType(fEltType.closure());
	}

	@Override
	public Type compose(Type other) {
		return TF.setType(fEltType.compose(other.getElementType()));
	}

	@Override
	public Type select(int... fields) {
		return TF.setType(fEltType.select(fields));
	}

	@Override
	public Type select(String... names) {
		return TF.setType(fEltType.select(names));
	}


	@Override
	public int hashCode() {
		return 56509 + 3511 * fEltType.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SetType)) {
			return false;
		}
		SetType other = (SetType) obj;
		// N.B.: The element type must have been created and canonicalized before
		// any
		// attempt to manipulate the outer type (i.e. SetType), so we can use object
		// identity here for the fEltType.
		return fEltType == other.fEltType;
	}

	@Override
	public String toString() {
		if (fEltType.isFixedWidth() && !fEltType.equivalent(VoidType.getInstance())) {
			StringBuilder sb = new StringBuilder();
			sb.append("rel[");
			int idx = 0;
			Iterator<Type> iter = fEltType.iterator();
			while(iter.hasNext()) {
				Type elemType = iter.next();
				if (idx++ > 0)
					sb.append(",");
				sb.append(elemType.toString());
				if (hasFieldNames()) {
					sb.append(" " + fEltType.getFieldName(idx - 1));
				}
			}
			sb.append("]");
			return sb.toString();
		}
		else {
			return "set[" + fEltType + "]";
		}
	}

	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitSet(this);
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
		return type.isSubtypeOfSet(this);
	}

	@Override
	public Type lub(Type other) {
		return other.lubWithSet(this);
	}

	@Override
	public Type glb(Type type) {
		return type.glbWithSet(this);
	}

	@Override
	protected Type glbWithSet(Type type) {
		return this == type ? this : TF.setType(fEltType.glb(type.getElementType()));
	}

	@Override
	protected boolean isSubtypeOfSet(Type type) {
		return fEltType.isSubtypeOf(type.getElementType());
	}

	@Override
	protected Type lubWithSet(Type type) {
		return TF.setType(fEltType.lub(type.getElementType()));
	}

	@Override
	public boolean isOpen() {
		return fEltType.isOpen();
	}

	@Override
	public boolean match(Type matched, Map<Type, Type> bindings) throws FactTypeUseException {
		return super.match(matched, bindings) && getElementType().match(matched.getElementType(), bindings);
	}

	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		return TypeFactory.getInstance().setType(getElementType().instantiate(bindings));
	}
}
