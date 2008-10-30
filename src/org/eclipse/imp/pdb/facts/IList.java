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

import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;

public interface IList extends Iterable<IValue>, IValue {
    public Type getElementType();
    
    public int length();
    public IList reverse();
    public IList append(IValue e) throws FactTypeError;
    public IList insert(IValue e) throws FactTypeError;
    public IValue get(int i);
    public boolean isEmpty();
   
    public IListWriter getWriter();
}
