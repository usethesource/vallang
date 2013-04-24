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

import org.eclipse.imp.pdb.facts.IContainer;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public class SetOrRel {

	@SuppressWarnings("unchecked")
	public static <ISetOrRel extends ISet> ISetOrRel apply(Type elementType, IContainer data) {
		Type calculatedElementType = data.isEmpty() ? TypeFactory.getInstance().voidType() : elementType;
		
		if (calculatedElementType.isTupleType())
			return (ISetOrRel) new Relation(calculatedElementType, data);
		else
			return (ISetOrRel) new Set(calculatedElementType, data);
	}
	
}
