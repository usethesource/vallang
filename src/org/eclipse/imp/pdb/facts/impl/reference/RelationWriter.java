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
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.reference;

import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/*package*/ class RelationWriter extends SetWriter implements IRelationWriter {

    /*package*/ RelationWriter(Type eltType) {
        super(eltType);
    }

    /*package*/ RelationWriter() {
        super();
    }

    public IRelation done() {
    	// Temporary fix of the static vs dynamic type issue
    	eltType = TypeFactory.getInstance().voidType();
    	for(IValue el : setContent)
    		eltType = eltType.lub(el.getType());
    	// ---
        if (constructedSet == null) {
            constructedSet = SetOrRel.apply(eltType, setContent);
        }
        return (IRelation) constructedSet;
    }

}
