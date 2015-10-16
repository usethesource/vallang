/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Jurgen Vinju - initial API and implementation
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.treasure.impl.primitive;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import io.usethesource.capsule.AbstractSpecialisedImmutableMap;
import io.usethesource.capsule.ImmutableMap;
import io.usethesource.treasure.IAnnotatable;
import io.usethesource.treasure.IConstructor;
import io.usethesource.treasure.IExternalValue;
import io.usethesource.treasure.IList;
import io.usethesource.treasure.INode;
import io.usethesource.treasure.IValue;
import io.usethesource.treasure.IWithKeywordParameters;
import io.usethesource.treasure.exceptions.FactTypeUseException;
import io.usethesource.treasure.impl.AbstractDefaultAnnotatable;
import io.usethesource.treasure.impl.AbstractDefaultWithKeywordParameters;
import io.usethesource.treasure.impl.AbstractValue;
import io.usethesource.treasure.impl.AnnotatedConstructorFacade;
import io.usethesource.treasure.impl.ConstructorWithKeywordParametersFacade;
import io.usethesource.treasure.type.ExternalType;
import io.usethesource.treasure.type.Type;
import io.usethesource.treasure.type.TypeFactory;
import io.usethesource.treasure.type.TypeStore;
import io.usethesource.treasure.visitors.IValueVisitor;

/**
 * See {@link IExternalValue}
 * <br>
 * Note that NORMAL USE OF THE PDB DOES NOT REQUIRE EXTENDING THIS CLASS.
 */
public abstract class ExternalValue implements IExternalValue {

	private final ExternalType type;

	@Override
	public boolean isEqual(IValue other) {
		return equals(other);
	}

	
	protected ExternalValue(ExternalType type) {
		this.type = type;
	}

	@Override
	public ExternalType getType() {
		return type;
	}

	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
		return v.visitExternal(this);
	}
}
