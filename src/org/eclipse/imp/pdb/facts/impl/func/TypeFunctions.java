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
package org.eclipse.imp.pdb.facts.impl.func;

import java.util.Iterator;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public class TypeFunctions {

    public static Type lub(Iterable<IValue> xs) {
        Type result = TypeFactory.getInstance().voidType();
        for (IValue x : xs) {
            result = result.lub(x.getType());
        }
        return result;
    }

    public static Type lub(Iterator<IValue> xs) {
        Type result = TypeFactory.getInstance().voidType();
        while (xs.hasNext()) {
        	IValue x = xs.next();
            result = result.lub(x.getType());
        }
        return result;
    }
    
    // NOTE: nice example of how to shorten code
//    def lub(xs: GenTraversable[IValue]): Type = {
//        xs.foldLeft(TypeFactory.getInstance.voidType)((t, x) => t lub x.getType)
//    }

}
