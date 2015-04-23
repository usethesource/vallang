/*******************************************************************************
* Copyright (c) 2009-2015 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - initial API and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.AbstractDefaultAnnotatable;
import org.eclipse.imp.pdb.facts.impl.AbstractDefaultWithKeywordParameters;
import org.eclipse.imp.pdb.facts.impl.AnnotatedConstructorFacade;
import org.eclipse.imp.pdb.facts.impl.ConstructorWithKeywordParametersFacade;
import org.eclipse.imp.pdb.facts.type.ExternalType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.util.AbstractSpecialisedImmutableMap;
import org.eclipse.imp.pdb.facts.util.ImmutableMap;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

/**
 * IExternalValue, together with {@link ExternalType} offer a limited form of extensibility
 * to the PDB's value and type system. The IExternalValue interface is used to tag extensions,
 * such as 'function values' that are not part of the PDB fact exchange and manipulation
 * interfaces but do need to integrate with them.
 * <br>
 * Note that implementations of IExternalValues are obliged to have a type that subclasses
 * ExternalType and that they all model IConstructor behavior via the default methods below, which
 * you should override for more useful behavior. If you do not do this, serialization will be lossy.
 * <br>
 * Note that NORMAL USE OF THE PDB DOES NOT REQUIRE IMPLEMENTING THIS INTERFACE
 */
public interface IExternalValue extends IConstructor {
	static final TypeStore store = new TypeStore();
	
	/**
	 * The default type of an external value is the class name of its implementation as an ADT.
	 */
	@Override
	default Type getType() {
		synchronized (store) {
			return TypeFactory.getInstance().abstractDataType(store, getName());
		}
	}

	/**
	 * The default constructor of an external value again the class name of its implementation.
	 * A default serialization of a class named, for example, `class Function implements IExternalValue { }` 
	 * would be the nullary constructor `Function()` of the ADT type `Function`. If you want to store more
	 * data there, then you override this method and the other methods below for getting data out of your representation.
	 */
	@Override
	default Type getConstructorType() {
		synchronized (store) {
			return TypeFactory.getInstance().constructor(store, getType(), getName());
		}
	}
	
	@Override
	default String getName() {
		return getConstructorType().getName();
	}
	
	@Override
	default Type getUninstantiatedConstructorType() {
		return getConstructorType();
	}
	
	@Override
	default IValue get(String label) {
		return null;
	}
	
	@Override
	default IConstructor set(String label, IValue newChild) throws FactTypeUseException {
		return this;
	}
	
	@Override
	default boolean has(String label) {
		return false;
	}
	
	@Override
	default IConstructor set(int index, IValue newChild) {
		return this;
	}
	
	@Override
	default Type getChildrenTypes() {
		return TypeFactory.getInstance().voidType();
	}
	
	@Override
	default boolean declaresAnnotation(TypeStore store, String label) {
		return false;
	}
	
	@Override
	default boolean isAnnotatable() {
		return true;
	}
	
	@Override
	default IAnnotatable<? extends IConstructor> asAnnotatable() {
		return new AbstractDefaultAnnotatable<IConstructor>(this) {
			@Override
			protected IConstructor wrap(IConstructor content,
					ImmutableMap<String, IValue> annotations) {
				return new AnnotatedConstructorFacade(content, annotations);
			}
		};
	}
	
	@Override
	default boolean mayHaveKeywordParameters() {
		return true;
	}
	
	@Override
	default INode replace(int first, int second, int end, IList repl)
			throws FactTypeUseException, IndexOutOfBoundsException {
		return this;
	}
	
	@Override
	default int arity() {
		return 0;
	}
	
	@Override
	default IValue get(int i) throws IndexOutOfBoundsException {
		return null;
	}
	
	@Override
	default Iterable<IValue> getChildren() {
		return Collections.emptyList();
	}
	
	
	@Override
	default Iterator<IValue> iterator() {
		return Collections.emptyIterator();
	}
	
	@Override
	default IWithKeywordParameters<IConstructor> asWithKeywordParameters() {
		 return new AbstractDefaultWithKeywordParameters<IConstructor>(this, AbstractSpecialisedImmutableMap.<String,IValue>mapOf()) {
			    @Override
			    protected IConstructor wrap(IConstructor content, ImmutableMap<String, IValue> parameters) {
			      return new ConstructorWithKeywordParametersFacade(content, parameters);
			    }
			    
			    @Override
			    public boolean hasParameters() {
			    	return false;
			    }

			    @Override
			    public java.util.Set<String> getParameterNames() {
			    	return Collections.emptySet();
			    }

			    @Override
			    public Map<String, IValue> getParameters() {
			    	return Collections.unmodifiableMap(parameters);
			    }
			  }; 
	}
	
	@Override
	default <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
		return v.visitExternal(this);
	}
}
