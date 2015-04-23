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
	@Override
	abstract ExternalType getType();

	@Override
	default Type getConstructorType() {
		throw new UnsupportedOperationException("add implementation to support features such as (de)serialization and pattern matching");
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
	default String getName() {
		return getClass().getName().replaceAll("\\.", "_");
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
}
