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

public interface ISet extends Iterable<IValue>, IValue {
	public Type getElementType();
	
    public boolean isEmpty();

    public int size();

    public boolean contains(IValue element) throws FactTypeError;

    public ISet insert(IValue element) throws FactTypeError;

    public ISet union(ISet set)  throws FactTypeError;
    
    public IRelation union(IRelation rel)  throws FactTypeError;

    public ISet intersect(ISet set)  throws FactTypeError;
    
    public IRelation intersect(IRelation rel) throws FactTypeError;

    public ISet subtract(ISet set)  throws FactTypeError;
    
    public IRelation subtract(IRelation set)  throws FactTypeError;

    public ISet invert(ISet universe)  throws FactTypeError;
    
    public IRelation invert(IRelation universe)  throws FactTypeError;

    public ISetWriter getWriter();
    
    public IRelation toRelation() throws FactTypeError;
    
    public IRelation product(IRelation rel);
    
    public IRelation product(ISet set);

	
}
