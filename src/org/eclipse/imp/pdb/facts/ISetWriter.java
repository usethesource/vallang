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


public interface ISetWriter {
    ISet getSet();
    void insert(IValue v) throws FactTypeError ;
    void insertAll(ISet other)  throws FactTypeError;
    void insertAll(IRelation relation)  throws FactTypeError;
    void insertAll(IList list) throws FactTypeError;
    void insertAll(Iterable<? extends IValue> collection) throws FactTypeError;
    void done();
   
}
