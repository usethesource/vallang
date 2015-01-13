/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

public abstract interface ITuple extends Iterable<IValue>, IValue {
    public IValue get(int i) throws IndexOutOfBoundsException;
    
    @Deprecated
    public IValue get(String label) throws FactTypeUseException;
    
    public ITuple set(int i, IValue arg) throws IndexOutOfBoundsException;
    
    @Deprecated
    public ITuple set(String label, IValue arg) throws FactTypeUseException;

    public int arity();

    public boolean equals(Object o);
    
    public IValue select(int... fields) throws IndexOutOfBoundsException;
    
    @Deprecated
    public IValue selectByFieldNames(String... fields) throws FactTypeUseException;
}
