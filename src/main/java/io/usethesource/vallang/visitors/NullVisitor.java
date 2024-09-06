/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    jurgen@vinju.org
*******************************************************************************/
package io.usethesource.vallang.visitors;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.IExternalValue;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;

/**
 * This abstract class does nothing except returning null. Extend it
 * to easily implement a visitor that visits selected types of IValues.
 * 
 */
public abstract class NullVisitor<@Nullable T, E extends Throwable> implements IValueVisitor<T, E> {
    @Override
    public T visitReal(IReal o)  throws E {
        return null;
    }

    @Override
    public T visitInteger(IInteger o)  throws E {
        return null;
    }

    @Override
    public T visitRational(IRational o)  throws E {
        return null;
    }

    @Override
    public T visitList(IList o)  throws E {
        return null;
    }

    @Override
    public T visitMap(IMap o)  throws E {
        return null;
    }

    @Override
    public T visitSet(ISet o)  throws E {
        return null;
    }

    @Override
    public T visitSourceLocation(ISourceLocation o)  throws E {
        return null;
    }

    @Override
    public T visitString(IString o)  throws E {
        return null;
    }

    @Override
    public T visitNode(INode o)  throws E {
        return null;
    }

    @Override
    public T visitConstructor(IConstructor o) throws E {
        return null;
    }
    
    @Override
    public T visitTuple(ITuple o)  throws E {
        return null;
    }
    
    @Override
    public T visitBoolean(IBool boolValue) throws E {
        return null;
    }
    
    @Override
    public T visitExternal(IExternalValue externalValue) throws E {
        return null;
    }
    
    @Override
    public T visitDateTime(IDateTime o) throws E {
        return null;
    }
}
