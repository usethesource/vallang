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

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public class ListOrRel {

	@SuppressWarnings("unchecked")
	public static <IListOrRel extends IList> IListOrRel apply(Type elementType, java.util.List<IValue> data) {
		Type calculatedElementType = data.isEmpty() ? TypeFactory.getInstance().voidType() : elementType;
		
		if (calculatedElementType.isTupleType())
			return (IListOrRel) new ListRelation(calculatedElementType, data);
		else
			return (IListOrRel) new List(calculatedElementType, data);
	}
	
}
