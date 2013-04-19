/*******************************************************************************
 * Copyright (c) 2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Jurgen J. Vinju - Jurgen.Vinju@cwi.nl - CWI
 *   * Paul Klint - Paul.Klint@cwi.nl - CWI
 *
 * Based on code by:
 *
 *   * Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.reference;

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.func.SetFunctions;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/*package*/ class Relation extends Set implements IRelation {

	/*package*/ Relation(Type elementType, java.util.Set<IValue> content) {
		super(elementType, content);
	}
	
	public int arity() {
		return getType().getArity();
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitRelation(this);
	}
	
	public Type getFieldTypes() {
		return getType().getFieldTypes();
	}

    @Override
    public IRelation compose(IRelation that) throws FactTypeUseException {
        return (IRelation) SetFunctions.compose(getValueFactory(), this, that);
    }

    @Override
    public IRelation closure() throws FactTypeUseException {
        return (IRelation) SetFunctions.closure(getValueFactory(), this);
    }

    @Override
    public IRelation closureStar() throws FactTypeUseException {
        return (IRelation) SetFunctions.closureStar(getValueFactory(), this);
    }

    @Override
    public ISet carrier() {
        return SetFunctions.carrier(getValueFactory(), this);
    }

    @Override
    public ISet domain() {
        return SetFunctions.domain(getValueFactory(), this);
    }

    @Override
    public ISet range() {
        return SetFunctions.range(getValueFactory(), this);
    }

    @Override
    public ISet select(int... fields) {
        return SetFunctions.project(getValueFactory(), this, fields);
    }

    @Override
    public ISet selectByFieldNames(String... fields) throws FactTypeUseException {
        return SetFunctions.projectByFieldNames(getValueFactory(), this, fields);
    }
}